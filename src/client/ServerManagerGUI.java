package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ServerManagerGUI extends JFrame {

    private static final Color PAGE_BACKGROUND = new Color(239, 243, 248);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(210, 226, 255);
    private static final Color TEXT_COLOR = new Color(33, 43, 60);
    private static final Color MUTED_TEXT = new Color(97, 110, 129);

    private static final NodeEndpoint[] DEFAULT_ENDPOINTS = new NodeEndpoint[]{
            new NodeEndpoint("localhost", 2001),
            new NodeEndpoint("localhost", 2002),
            new NodeEndpoint("localhost", 2003),
            new NodeEndpoint("localhost", 2004),
            new NodeEndpoint("localhost", 2005),
            new NodeEndpoint("localhost", 2006)
    };

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;
    private static final int AUTO_REFRESH_MS = 1000;
    private static final int MAX_AGGREGATE_LINES = 120;

    private final NodeEndpoint[] endpoints;
    private final String[] cachedLogs;
    private final boolean[] serverOnline;
    private final String[] cachedStatuses;
    private final Timer autoRefreshTimer;

    private JComboBox<String> serverSelector;
    private JTextPane aggregateLogPane;
    private JTextArea selectedServerArea;
    private JCheckBox autoRefreshCheckBox;
    private RingTopologyPanel ringPanel;
    private JLabel activeCountValue;
    private JLabel tokenValue;

    public ServerManagerGUI() {
        this.endpoints = loadEndpoints();
        this.cachedLogs = new String[endpoints.length];
        this.serverOnline = new boolean[endpoints.length];
        this.cachedStatuses = new String[endpoints.length];
        this.autoRefreshTimer = new Timer(AUTO_REFRESH_MS, e -> refreshLogs(false));

        setTitle("GUI Quan Ly Server");
        setSize(1380, 880);
        setMinimumSize(new Dimension(1180, 780));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setContentPane(buildPage());

        refreshLogs(true);
        autoRefreshTimer.start();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildPage() {
        JPanel page = new JPanel(new BorderLayout(18, 18));
        page.setBackground(PAGE_BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        page.add(buildHeader(), BorderLayout.NORTH);
        page.add(buildContent(), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildHeader() {
        JPanel header = createCard(new BorderLayout(8, 8));

        JLabel title = new JLabel("Dashboard giam sat 6 server trong token ring");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(TEXT_COLOR);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);

        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout(18, 18));
        topRow.setOpaque(false);
        topRow.setPreferredSize(new Dimension(0, 520));
        topRow.add(buildTopologyCard(), BorderLayout.WEST);
        topRow.add(buildControlCard(), BorderLayout.CENTER);

        content.add(topRow, BorderLayout.NORTH);
        content.add(buildAggregateCard(), BorderLayout.CENTER);
        return content;
    }

    private JPanel buildTopologyCard() {
        JPanel card = createCard(new BorderLayout(12, 12));
        card.setPreferredSize(new Dimension(620, 520));

        JLabel title = new JLabel("So do vong 6 server");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(TEXT_COLOR);

        JPanel metricPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        metricPanel.setOpaque(false);
        metricPanel.add(createMetricCard("Server online", "0/6"));
        metricPanel.add(createMetricCard("Token hien tai", "Dang tim"));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(metricPanel);

        ringPanel = new RingTopologyPanel();
        ringPanel.setPreferredSize(new Dimension(560, 390));
        ringPanel.setCenterLabel("RING");
        ringPanel.setSubtitle("");
        ringPanel.setShowTokenBadge(false);
        ringPanel.setShowCenterCaption(false);
        ringPanel.setVisualScale(0.82f);

        card.add(top, BorderLayout.NORTH);
        card.add(ringPanel, BorderLayout.CENTER);
        card.add(createLegendPanel(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildControlCard() {
        JPanel card = createCard(new BorderLayout(12, 12));

        JLabel title = new JLabel("Dieu khien man hinh giam sat");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(TEXT_COLOR);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actions.setOpaque(false);

        serverSelector = new JComboBox<>(buildNodeChoices());
        serverSelector.setSelectedIndex(0);
        serverSelector.addActionListener(e -> {
            ringPanel.setSelectedIndex(serverSelector.getSelectedIndex());
            updateSelectedServerArea();
        });

        JButton refreshButton = createButton("Lam Moi Dashboard", PRIMARY_COLOR);
        refreshButton.addActionListener(e -> refreshLogs(true));

        autoRefreshCheckBox = new JCheckBox("Tu dong lam moi moi 1 giay");
        autoRefreshCheckBox.setSelected(true);
        autoRefreshCheckBox.addActionListener(e -> toggleAutoRefresh());

        actions.add(new JLabel("Chon server:"));
        actions.add(serverSelector);
        actions.add(refreshButton);
        actions.add(autoRefreshCheckBox);

        JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        notePanel.setOpaque(false);
        JLabel note = new JLabel("Log tong hop da duoc tron theo thoi gian de nhin thay token chay qua tung node.");
        note.setForeground(new Color(64, 85, 116));
        notePanel.add(note);

        JPanel detailPanel = buildEmbeddedDetailPanel();

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(actions, BorderLayout.NORTH);
        centerPanel.add(detailPanel, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(notePanel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAggregateCard() {
        JPanel card = createCard(new BorderLayout(12, 12));

        JLabel title = new JLabel("Dong log tong hop cua toan he thong");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(TEXT_COLOR);

        aggregateLogPane = new JTextPane();
        aggregateLogPane.setEditable(false);
        aggregateLogPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        aggregateLogPane.setBackground(new Color(247, 249, 252));
        aggregateLogPane.setForeground(TEXT_COLOR);
        aggregateLogPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(aggregateLogPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(224, 229, 236), 1));

        card.add(title, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildEmbeddedDetailPanel() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setOpaque(false);

        JLabel title = new JLabel("Log chi tiet theo server dang chon");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(TEXT_COLOR);

        selectedServerArea = new JTextArea();
        selectedServerArea.setEditable(false);
        selectedServerArea.setLineWrap(true);
        selectedServerArea.setWrapStyleWord(true);
        selectedServerArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        selectedServerArea.setBackground(new Color(247, 249, 252));
        selectedServerArea.setForeground(TEXT_COLOR);
        selectedServerArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(selectedServerArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(224, 229, 236), 1));

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 236), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.add(title, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCard(BorderLayout layout) {
        JPanel card = new JPanel(layout);
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 227, 234), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        return card;
    }

    private JPanel createMetricCard(String title, String initialValue) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(new Color(247, 249, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 236), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED_TEXT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setForeground(TEXT_COLOR);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));

        if ("Server online".equals(title)) {
            activeCountValue = valueLabel;
        } else if ("Token hien tai".equals(title)) {
            tokenValue = valueLabel;
        }

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setBackground(color);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        return button;
    }

    private JPanel createLegendPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legend.setOpaque(false);
        legend.add(createLegendItem(new Color(243, 120, 67), "Server dang chon"));
        legend.add(createLegendItem(new Color(255, 201, 71), "Dang giu token"));
        legend.add(createLegendItem(new Color(46, 184, 92), "Dang hoat dong"));
        legend.add(createLegendItem(new Color(225, 78, 78), "Tam mat ket noi"));
        return legend;
    }

    private JPanel createLegendItem(Color color, String labelText) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 228, 236), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JPanel dot = new JPanel();
        dot.setOpaque(true);
        dot.setBackground(color);
        dot.setPreferredSize(new Dimension(12, 12));
        dot.setBorder(BorderFactory.createLineBorder(color.darker(), 1));

        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_COLOR);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));

        item.add(dot);
        item.add(label);
        return item;
    }

    private String[] buildNodeChoices() {
        String[] nodeChoices = new String[endpoints.length];
        for (int i = 0; i < endpoints.length; i++) {
            nodeChoices[i] = "Server " + (i + 1);
        }
        return nodeChoices;
    }

    private void refreshLogs(boolean announce) {
        runInBackground(() -> {
            if (announce) {
                SwingUtilities.invokeLater(() -> aggregateLogPane.setText("Dang lay log moi tu 6 server..."));
            }

            for (int i = 0; i < endpoints.length; i++) {
                cachedLogs[i] = sendCommandToNode(i, "LOGS");
                cachedStatuses[i] = sendCommandToNode(i, "STATUS");
                String currentStatus = cachedStatuses[i];
                serverOnline[i] = currentStatus != null
                        && currentStatus.startsWith("STATUS|");
            }

            updateTopology();
            updateAggregateArea();
            updateSelectedServerArea();
        });
    }

    private void updateTopology() {
        int onlineCount = 0;
        int tokenHolderIndex = -1;
        for (boolean online : serverOnline) {
            if (online) {
                onlineCount++;
            }
        }
        for (int i = 0; i < cachedStatuses.length; i++) {
            String status = cachedStatuses[i];
            if (status != null && status.contains("|hasToken=true")) {
                tokenHolderIndex = i;
                break;
            }
        }

        int selectedIndex = serverSelector == null ? 0 : serverSelector.getSelectedIndex();
        final int finalTokenHolderIndex = tokenHolderIndex;
        final int finalOnlineCount = onlineCount;
        SwingUtilities.invokeLater(() -> {
            ringPanel.setOnlineStates(serverOnline);
            ringPanel.setSelectedIndex(selectedIndex);
            ringPanel.setTokenHolderIndex(finalTokenHolderIndex);
            activeCountValue.setText(finalOnlineCount + "/" + endpoints.length);
            tokenValue.setText(finalTokenHolderIndex >= 0 ? "Server " + (finalTokenHolderIndex + 1) : "Dang tim");
        });
    }

    private void updateAggregateArea() {
        List<String> mergedLines = new ArrayList<>();
        for (String cachedLog : cachedLogs) {
            String[] importantLines = extractImportantLines(cachedLog);
            Collections.addAll(mergedLines, importantLines);
        }

        Collections.sort(mergedLines);
        if (mergedLines.size() > MAX_AGGREGATE_LINES) {
            mergedLines = new ArrayList<>(mergedLines.subList(mergedLines.size() - MAX_AGGREGATE_LINES, mergedLines.size()));
        }

        String content = mergedLines.isEmpty() ? "CHUA_CO_LOG" : String.join("\n", mergedLines);
        SwingUtilities.invokeLater(() -> {
            setAggregateLogContent(content);
            aggregateLogPane.setCaretPosition(aggregateLogPane.getDocument().getLength());
        });
    }

    private void updateSelectedServerArea() {
        int selectedIndex = serverSelector == null ? 0 : serverSelector.getSelectedIndex();
        String logContent = cachedLogs[selectedIndex];
        if (logContent == null || logContent.trim().isEmpty()) {
            logContent = "CHUA_CO_LOG";
        } else if ("LOI|Lenh khong hop le".equalsIgnoreCase(logContent.trim())) {
            logContent = "Server nay dang chay ban cu, chua ho tro lenh LOGS.\n"
                    + "Can compile lai va redeploy 6 server Railway de dashboard doc duoc log.";
        }

        String finalLogContent = logContent;
        SwingUtilities.invokeLater(() -> {
            selectedServerArea.setText(finalLogContent);
            selectedServerArea.setCaretPosition(selectedServerArea.getDocument().getLength());
        });
    }

    private String[] extractImportantLines(String rawLog) {
        if (rawLog == null || rawLog.trim().isEmpty()) {
            return new String[0];
        }

        if ("LOI|Lenh khong hop le".equalsIgnoreCase(rawLog.trim())) {
            return new String[0];
        }

        if (rawLog.startsWith("LOI|")) {
            return new String[]{rawLog};
        }

        String[] lines = rawLog.split("\\R");
        List<String> importantLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                importantLines.add(trimmed);
            }
        }
        return importantLines.toArray(new String[0]);
    }

    private void toggleAutoRefresh() {
        if (autoRefreshCheckBox.isSelected()) {
            autoRefreshTimer.start();
        } else {
            autoRefreshTimer.stop();
        }
    }

    private void setAggregateLogContent(String content) {
        StyledDocument doc = aggregateLogPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            String[] lines = content.split("\\R");
            for (String line : lines) {
                SimpleAttributeSet style = new SimpleAttributeSet();
                StyleConstants.setFontFamily(style, Font.MONOSPACED);
                StyleConstants.setFontSize(style, 13);

                String lower = line.toLowerCase();
                if (lower.contains("da nhan token")) {
                    StyleConstants.setForeground(style, new Color(43, 105, 176));
                    StyleConstants.setBold(style, true);
                } else if (lower.contains("da chuyen token")) {
                    StyleConstants.setForeground(style, new Color(64, 137, 95));
                    StyleConstants.setBold(style, true);
                } else if (lower.contains("dang in job")) {
                    StyleConstants.setForeground(style, new Color(168, 102, 24));
                } else if (lower.contains("da dua job in")) {
                    StyleConstants.setForeground(style, new Color(114, 93, 193));
                } else if (lower.contains("loi|") || lower.contains("khong the")) {
                    StyleConstants.setForeground(style, new Color(184, 63, 80));
                    StyleConstants.setBold(style, true);
                } else {
                    StyleConstants.setForeground(style, TEXT_COLOR);
                }

                doc.insertString(doc.getLength(), line + "\n", style);
            }
        } catch (Exception ignored) {
            aggregateLogPane.setText(content);
        }
    }

    private String sendCommandToNode(int nodeIndex, String command) {
        NodeEndpoint endpoint = endpoints[nodeIndex];

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host, endpoint.port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(command);

                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(line);
                }

                if (builder.length() == 0) {
                    return "(khong co phan hoi)";
                }

                return builder.toString();
            }
        } catch (SocketTimeoutException ex) {
            return "LOI|Het thoi gian cho phan hoi tu " + endpoint.host + ":" + endpoint.port;
        } catch (Exception ex) {
            return "LOI|Khong ket noi duoc toi " + endpoint.host + ":" + endpoint.port + " - " + ex.getMessage();
        }
    }

    private void runInBackground(Runnable task) {
        Thread worker = new Thread(task, "server-manager-dashboard-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private NodeEndpoint[] loadEndpoints() {
        String configuredNodes = System.getenv("NODE_ADDRESSES");
        if (configuredNodes == null || configuredNodes.trim().isEmpty()) {
            return DEFAULT_ENDPOINTS;
        }

        String[] rawNodes = configuredNodes.split(",");
        NodeEndpoint[] parsed = new NodeEndpoint[rawNodes.length];
        for (int i = 0; i < rawNodes.length; i++) {
            parsed[i] = parseEndpoint(rawNodes[i].trim());
        }
        return parsed;
    }

    private NodeEndpoint parseEndpoint(String rawValue) {
        if (rawValue.contains("://")) {
            URI uri = URI.create(rawValue);
            int port = uri.getPort() > 0 ? uri.getPort() : 8080;
            return new NodeEndpoint(uri.getHost(), port);
        }

        if (rawValue.contains(":")) {
            int lastColon = rawValue.lastIndexOf(':');
            String host = rawValue.substring(0, lastColon).trim();
            int port = Integer.parseInt(rawValue.substring(lastColon + 1).trim());
            return new NodeEndpoint(host, port);
        }

        return new NodeEndpoint(rawValue, 8080);
    }

    public static void main(String[] args) {
        applyLookAndFeel();
        SwingUtilities.invokeLater(ServerManagerGUI::new);
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static class NodeEndpoint {
        private final String host;
        private final int port;

        private NodeEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
