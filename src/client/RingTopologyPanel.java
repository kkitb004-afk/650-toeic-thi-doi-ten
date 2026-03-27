package client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.JPanel;
import javax.swing.Timer;

public class RingTopologyPanel extends JPanel {

    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color RING_COLOR = new Color(173, 182, 196);
    private static final Color NODE_COLOR = new Color(123, 140, 167);
    private static final Color NODE_TEXT_COLOR = new Color(34, 48, 74);
    private static final Color NODE_SELECTED_COLOR = new Color(242, 120, 75);
    private static final Color NODE_ONLINE_RING = new Color(54, 179, 126);
    private static final Color NODE_OFFLINE_RING = new Color(198, 71, 88);
    private static final Color CENTER_COLOR = new Color(33, 83, 188);
    private static final Color TOKEN_COLOR = new Color(255, 198, 64);
    private static final Color TOKEN_GLOW = new Color(255, 220, 120, 130);
    private static final Color TOKEN_LABEL_COLOR = new Color(144, 96, 12);
    private static final Color TOKEN_LABEL_BG = new Color(255, 241, 199);
    private static final int NODE_COUNT = 6;

    private final Timer animationTimer;
    private int selectedIndex = 0;
    private int tokenHolderIndex = -1;
    private boolean[] onlineStates = new boolean[]{true, true, true, true, true, true};
    private String centerLabel = "TOKEN RING";
    private String subtitle = "6 server dang tao thanh vong ao";
    private boolean showTokenBadge = true;
    private boolean showCenterCaption = true;
    private float visualScale = 1.0f;
    private float animationPhase = 0f;

    public RingTopologyPanel() {
        setOpaque(true);
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(420, 420));
        animationTimer = new Timer(40, e -> {
            animationPhase += 0.02f;
            if (animationPhase > 1f) {
                animationPhase = 0f;
            }
            repaint();
        });
        animationTimer.start();
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, NODE_COUNT - 1));
        repaint();
    }

    public void setTokenHolderIndex(int tokenHolderIndex) {
        this.tokenHolderIndex = tokenHolderIndex;
        repaint();
    }

    public void setOnlineStates(boolean[] onlineStates) {
        if (onlineStates == null || onlineStates.length != NODE_COUNT) {
            return;
        }
        this.onlineStates = onlineStates.clone();
        repaint();
    }

    public void setCenterLabel(String centerLabel) {
        this.centerLabel = centerLabel;
        repaint();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        repaint();
    }

    public void setShowTokenBadge(boolean showTokenBadge) {
        this.showTokenBadge = showTokenBadge;
        repaint();
    }

    public void setShowCenterCaption(boolean showCenterCaption) {
        this.showCenterCaption = showCenterCaption;
        repaint();
    }

    public void setVisualScale(float visualScale) {
        this.visualScale = Math.max(0.65f, Math.min(1.0f, visualScale));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int topPadding = 34;
        int sidePadding = 30;
        int bottomPadding = 52;
        int badgePadding = tokenHolderIndex >= 0 ? 22 : 8;

        int availableWidth = Math.max(180, width - sidePadding * 2);
        int availableHeight = Math.max(180, height - topPadding - bottomPadding - badgePadding);

        int centerX = sidePadding + availableWidth / 2;
        int centerY = topPadding + badgePadding + availableHeight / 2;
        int baseRingRadius = Math.max(110, Math.min(availableWidth, availableHeight) / 2 - 12);
        int ringRadius = Math.max(82, Math.round(baseRingRadius * visualScale));
        int nodeRadius = Math.max(22, Math.round(Math.min(32, ringRadius / 3f) * visualScale));
        int centerRadius = Math.max(28, Math.round(Math.max(34, nodeRadius + 2) * visualScale));

        double[][] positions = new double[NODE_COUNT][2];
        for (int i = 0; i < NODE_COUNT; i++) {
            double angle = Math.toRadians(-90 + i * (360.0 / NODE_COUNT));
            double x = centerX + Math.cos(angle) * ringRadius;
            double y = centerY + Math.sin(angle) * ringRadius;
            positions[i][0] = x;
            positions[i][1] = y;
        }

        g2.setColor(RING_COLOR);
        g2.setStroke(new BasicStroke(3.2f * visualScale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < NODE_COUNT; i++) {
            int next = (i + 1) % NODE_COUNT;
            g2.draw(new Line2D.Double(positions[i][0], positions[i][1], positions[next][0], positions[next][1]));
        }

        drawAnimatedToken(g2, positions, nodeRadius);

        Ellipse2D.Double centerCircle = new Ellipse2D.Double(
                centerX - centerRadius, centerY - centerRadius, centerRadius * 2.0, centerRadius * 2.0);
        g2.setColor(CENTER_COLOR);
        g2.fill(centerCircle);
        if (showCenterCaption) {
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 16f * visualScale));
            drawCenteredText(g2, centerLabel, centerX, centerY - 4);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f * visualScale));
            drawCenteredText(g2, "Virtual Circle", centerX, centerY + Math.round(15 * visualScale));
        } else {
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f * visualScale));
            drawCenteredText(g2, centerLabel, centerX, centerY + 1);
        }

        for (int i = 0; i < NODE_COUNT; i++) {
            double x = positions[i][0];
            double y = positions[i][1];
            Color nodeFill = i == selectedIndex ? NODE_SELECTED_COLOR : NODE_COLOR;
            Color statusColor = onlineStates[i] ? NODE_ONLINE_RING : NODE_OFFLINE_RING;

            if (i == tokenHolderIndex) {
                int pulseRadius = (int) (nodeRadius + 12 * visualScale + 6 * visualScale * Math.sin(animationPhase * Math.PI * 2));
                Ellipse2D.Double glowCircle = new Ellipse2D.Double(
                        x - pulseRadius, y - pulseRadius, pulseRadius * 2.0, pulseRadius * 2.0);
                g2.setColor(TOKEN_GLOW);
                g2.fill(glowCircle);
            }

            Ellipse2D.Double outerCircle = new Ellipse2D.Double(
                    x - nodeRadius - 5 * visualScale, y - nodeRadius - 5 * visualScale,
                    (nodeRadius + 5 * visualScale) * 2.0, (nodeRadius + 5 * visualScale) * 2.0);
            g2.setColor(statusColor);
            g2.fill(outerCircle);

            Ellipse2D.Double nodeCircle = new Ellipse2D.Double(
                    x - nodeRadius, y - nodeRadius, nodeRadius * 2.0, nodeRadius * 2.0);
            g2.setColor(nodeFill);
            g2.fill(nodeCircle);

            if (i == tokenHolderIndex) {
                g2.setColor(TOKEN_COLOR);
                g2.setStroke(new BasicStroke(3.2f * visualScale));
                g2.draw(new Ellipse2D.Double(x - nodeRadius - 3 * visualScale, y - nodeRadius - 3 * visualScale,
                        (nodeRadius + 3 * visualScale) * 2.0, (nodeRadius + 3 * visualScale) * 2.0));
                if (showTokenBadge) {
                    drawTokenBadge(g2, (int) x, (int) (y - nodeRadius - 24 * visualScale));
                }
            }

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 17f * visualScale));
            drawCenteredText(g2, String.valueOf(i + 1), (int) x, (int) y + 1);

            g2.setColor(NODE_TEXT_COLOR);
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f * visualScale));
            int labelX = (int) x;
            int labelY;
            switch (i) {
                case 0:
                    labelY = (int) (y - nodeRadius - 16 * visualScale);
                    break;
                case 1:
                    labelX += Math.round(30 * visualScale);
                    labelY = (int) (y - nodeRadius - 14 * visualScale);
                    break;
                case 2:
                    labelX += Math.round(26 * visualScale);
                    labelY = (int) (y + nodeRadius + 24 * visualScale);
                    break;
                case 3:
                    labelY = (int) (y + nodeRadius + 26 * visualScale);
                    break;
                case 4:
                    labelX -= Math.round(26 * visualScale);
                    labelY = (int) (y + nodeRadius + 24 * visualScale);
                    break;
                case 5:
                    labelX -= Math.round(30 * visualScale);
                    labelY = (int) (y - nodeRadius - 14 * visualScale);
                    break;
                default:
                    labelY = (int) (y + nodeRadius + 18 * visualScale);
                    break;
            }
            drawCenteredText(g2, "Server " + (i + 1), labelX, labelY);
        }

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            g2.setColor(new Color(76, 90, 112));
            g2.setFont(getFont().deriveFont(Font.PLAIN, 13f * visualScale));
            drawCenteredText(g2, subtitle, centerX, height - 16);
        }
        g2.dispose();
    }

    private void drawAnimatedToken(Graphics2D g2, double[][] positions, int nodeRadius) {
        if (tokenHolderIndex < 0 || tokenHolderIndex >= NODE_COUNT) {
            return;
        }

        int nextIndex = (tokenHolderIndex + 1) % NODE_COUNT;
        double startX = positions[tokenHolderIndex][0];
        double startY = positions[tokenHolderIndex][1];
        double endX = positions[nextIndex][0];
        double endY = positions[nextIndex][1];

        double progress = animationPhase;
        double tokenX = startX + (endX - startX) * progress;
        double tokenY = startY + (endY - startY) * progress;

        g2.setStroke(new BasicStroke(3f * visualScale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 214, 102, 110));
        g2.draw(new Line2D.Double(startX, startY, tokenX, tokenY));

        int tokenSize = Math.max(12, Math.round(20 * visualScale));
        int glowSize = Math.max(22, Math.round(32 * visualScale));
        Ellipse2D.Double token = new Ellipse2D.Double(tokenX - tokenSize / 2.0, tokenY - tokenSize / 2.0, tokenSize, tokenSize);
        g2.setColor(TOKEN_GLOW);
        g2.fill(new Ellipse2D.Double(tokenX - glowSize / 2.0, tokenY - glowSize / 2.0, glowSize, glowSize));
        g2.setColor(TOKEN_COLOR);
        g2.fill(token);
        g2.setColor(new Color(168, 110, 18));
        g2.setStroke(new BasicStroke(2f * visualScale));
        g2.draw(token);
    }

    private void drawTokenBadge(Graphics2D g2, int centerX, int centerY) {
        String label = "TOKEN HERE";
        Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(Font.BOLD, 11f));
        FontMetrics metrics = g2.getFontMetrics();
        int width = metrics.stringWidth(label) + 18;
        int height = 20;
        int x = centerX - width / 2;
        int y = centerY - height / 2;

        g2.setColor(TOKEN_LABEL_BG);
        g2.fillRoundRect(x, y, width, height, 14, 14);
        g2.setColor(TOKEN_LABEL_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, width, height, 14, 14);
        g2.drawString(label, centerX - metrics.stringWidth(label) / 2, y + height - 6);
        g2.setFont(originalFont);
    }

    private void drawCenteredText(Graphics2D g2, String text, int x, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x - metrics.stringWidth(text) / 2;
        int textY = y + metrics.getAscent() / 2 - 2;
        g2.drawString(text, textX, textY);
    }
}
