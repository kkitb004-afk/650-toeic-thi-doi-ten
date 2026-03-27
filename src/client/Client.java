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
import java.net.Socket;
import java.net.URI;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;

public class Client extends JFrame {

    private static final Color PAGE_BACKGROUND = new Color(239, 243, 248);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(210, 226, 255);
    private static final Color SECONDARY_COLOR = new Color(255, 224, 205);
    private static final Color TEXT_COLOR = new Color(33, 43, 60);
    private static final Color MUTED_TEXT = new Color(97, 110, 129);
    private static final int STATUS_REFRESH_MS = 1200;

    private static final NodeEndpoint[] DEFAULT_ENDPOINTS = new NodeEndpoint[]{
            new NodeEndpoint("localhost", 2001),
            new NodeEndpoint("localhost", 2002),
            new NodeEndpoint("localhost", 2003),
            new NodeEndpoint("localhost", 2004),
            new NodeEndpoint("localhost", 2005),
            new NodeEndpoint("localhost", 2006)
    };

    private final NodeEndpoint[] endpoints;
    private final boolean[] serverOnline;
    private final Timer statusRefreshTimer;
    private JComboBox<String> nodeSelector;
    private JTextField jobIdField;
    private JTextField contentField;
    private JTextArea resultArea;
    private RingTopologyPanel ringPanel;
    private JLabel topologyHelperLabel;

    public Client() {
        this.endpoints = loadEndpoints();
        this.serverOnline = new boolean[endpoints.length];
        this.statusRefreshTimer = new Timer(STATUS_REFRESH_MS, e -> refreshRingStatus());

        setTitle("Client In Phan Tan");
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildPage());
        setLocationRelativeTo(null);
        setVisible(true);
        refreshRingStatus();
        statusRefreshTimer.start();
    }

    private JPanel buildPage() {
        JPanel page = new JPanel(new BorderLayout(18, 18));
        page.setBackground(PAGE_BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        page.add(buildHeader(), BorderLayout.NORTH);
        page.add(buildMainContent(), BorderLayout.CENTER);
        page.add(buildResultCard(), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildHeader() {
        JPanel header = createCard(new BorderLayout(8, 8));

        JLabel title = new JLabel("Client dieu khien he thong 6 may in");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(TEXT_COLOR);

        JLabel subtitle = new JLabel("Chon server trong vong, gui lenh in va theo doi phan hoi ngay tren cung mot man hinh.");
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildMainContent() {
        JPanel content = new JPanel(new GridLayout(1, 2, 18, 18));
        content.setOpaque(false);
        content.add(buildControlCard());
        content.add(buildTopologyCard());
        return content;
    }

    private JPanel buildControlCard() {
        JPanel card = createCard(new BorderLayout(12, 12));

        JLabel sectionTitle = new JLabel("Gui lenh toi server duoc chon");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 20f));
        sectionTitle.setForeground(TEXT_COLOR);

        JLabel helper = new JLabel("Server duoc chon ben duoi se duoc to sang tren so do vong ben phai.");
        helper.setForeground(MUTED_TEXT);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(sectionTitle);
        top.add(helper);

        JPanel form = new JPanel(new GridLayout(3, 1, 12, 14));
        form.setOpaque(false);

        form.add(createFieldBlock("Chon server", createNodeSelector()));
        form.add(createFieldBlock("Ma job", createJobField()));
        form.add(createFieldBlock("Noi dung tai lieu", createContentField()));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setOpaque(false);

        JButton printButton = createButton("In Ngay", PRIMARY_COLOR);
        JButton cancelButton = createButton("Huy Lenh", SECONDARY_COLOR);
        JButton queryButton = createButton("Xem Lich Su", new Color(225, 224, 255));
        JButton statusButton = createButton("Kiem Tra Vong", new Color(215, 241, 229));

        printButton.addActionListener(e -> submitPrintJob());
        cancelButton.addActionListener(e -> cancelJob());
        queryButton.addActionListener(e -> sendCommand("QUERY", "Nhat ky job"));
        statusButton.addActionListener(e -> sendCommand("STATUS", "Trang thai vong"));

        buttonPanel.add(printButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(queryButton);
        buttonPanel.add(statusButton);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        JButton serverGuiButton = createButton("Mo Dashboard Giam Sat", new Color(230, 221, 255));
        serverGuiButton.addActionListener(e -> new ServerManagerGUI());
        footer.add(serverGuiButton);

        card.add(top, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildTopologyCard() {
        JPanel card = createCard(new BorderLayout(12, 12));

        JLabel sectionTitle = new JLabel("Truc quan hoa token ring");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 20f));
        sectionTitle.setForeground(TEXT_COLOR);

        topologyHelperLabel = new JLabel("So do vong duoi day giup theo doi nhanh server dang chon, server online va vi tri token.");
        topologyHelperLabel.setForeground(MUTED_TEXT);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(sectionTitle);
        top.add(topologyHelperLabel);

        ringPanel = new RingTopologyPanel();
        ringPanel.setCenterLabel("CLIENT");
        ringPanel.setSubtitle("Cam: server dang chon | Vang: server dang giu token | Xanh/Do: online-offline");
        ringPanel.setSelectedIndex(0);

        card.add(top, BorderLayout.NORTH);
        card.add(ringPanel, BorderLayout.CENTER);
        card.add(createLegendPanel(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildResultCard() {
        JPanel card = createCard(new BorderLayout(12, 12));

        JLabel sectionTitle = new JLabel("Phan hoi tu server");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 18f));
        sectionTitle.setForeground(TEXT_COLOR);

        resultArea = new JTextArea(10, 20);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultArea.setBackground(new Color(247, 249, 252));
        resultArea.setForeground(TEXT_COLOR);
        resultArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(224, 229, 236), 1));

        card.add(sectionTitle, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JComboBox<String> createNodeSelector() {
        nodeSelector = new JComboBox<>(buildNodeChoices());
        nodeSelector.setSelectedIndex(0);
        nodeSelector.addActionListener(e -> ringPanel.setSelectedIndex(nodeSelector.getSelectedIndex()));
        styleFormField(nodeSelector);
        return nodeSelector;
    }

    private JTextField createJobField() {
        jobIdField = new JTextField();
        styleFormField(jobIdField);
        return jobIdField;
    }

    private JTextField createContentField() {
        contentField = new JTextField();
        styleFormField(contentField);
        return contentField;
    }

    private JPanel createFieldBlock(String labelText, java.awt.Component field) {
        JPanel block = new JPanel(new BorderLayout(6, 6));
        block.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));

        block.add(label, BorderLayout.NORTH);
        block.add(field, BorderLayout.CENTER);
        return block;
    }

    private void styleFormField(JComponent field) {
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 15f));
        field.setPreferredSize(new Dimension(120, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 216, 228), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        if (field instanceof JTextField) {
            field.setBackground(new Color(249, 251, 255));
            field.setForeground(TEXT_COLOR);
            ((JTextField) field).setCaretColor(TEXT_COLOR);
        } else if (field instanceof JComboBox) {
            field.setBackground(new Color(249, 251, 255));
            field.setForeground(TEXT_COLOR);
            ((JComboBox<?>) field).setOpaque(true);
        }
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

    private JPanel createCard(BorderLayout layout) {
        JPanel card = new JPanel(layout);
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 227, 234), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        return card;
    }

    private String[] buildNodeChoices() {
        String[] nodeChoices = new String[endpoints.length];
        for (int i = 0; i < endpoints.length; i++) {
            nodeChoices[i] = "Server " + (i + 1);
        }
        return nodeChoices;
    }

    private void submitPrintJob() {
        String jobId = jobIdField.getText().trim();
        String content = contentField.getText().trim();
        if (jobId.isEmpty() || content.isEmpty()) {
            resultArea.append("Loi: Ma job va noi dung tai lieu khong duoc de trong\n");
            return;
        }

        sendCommand("PRINT|" + jobId + "|" + content, "Gui job in");
    }

    private void cancelJob() {
        String jobId = jobIdField.getText().trim();
        if (jobId.isEmpty()) {
            resultArea.append("Loi: Ma job khong duoc de trong\n");
            return;
        }

        sendCommand("CANCEL|" + jobId, "Huy job");
    }

    private void sendCommand(String command, String label) {
        int selectedIndex = nodeSelector.getSelectedIndex();
        int selectedNode = selectedIndex + 1;
        NodeEndpoint endpoint = endpoints[selectedIndex];

        try (Socket socket = new Socket(endpoint.host, endpoint.port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(command);
            resultArea.append("\n[Server " + selectedNode + "] " + label + ":\n");

            String line;
            boolean hasOutput = false;
            while ((line = in.readLine()) != null) {
                resultArea.append(line + "\n");
                hasOutput = true;
            }

            if (!hasOutput) {
                resultArea.append("(khong co phan hoi)\n");
            }

            resultArea.append("----------------------------------------\n");
        } catch (Exception ex) {
            resultArea.append("Loi ket noi toi Server " + selectedNode + ": " + ex.getMessage() + "\n");
        }

        resultArea.setCaretPosition(resultArea.getDocument().getLength());
        refreshRingStatus();
    }

    private void refreshRingStatus() {
        Thread worker = new Thread(() -> {
            boolean[] onlineStates = new boolean[endpoints.length];
            int tokenHolderIndex = -1;

            for (int i = 0; i < endpoints.length; i++) {
                String status = requestStatus(i);
                if (status.startsWith("STATUS|")) {
                    onlineStates[i] = true;
                    if (status.contains("|hasToken=true")) {
                        tokenHolderIndex = i;
                    }
                }
            }

            final int finalTokenHolderIndex = tokenHolderIndex;
            SwingUtilities.invokeLater(() -> {
                System.arraycopy(onlineStates, 0, serverOnline, 0, onlineStates.length);
                ringPanel.setOnlineStates(serverOnline);
                ringPanel.setTokenHolderIndex(finalTokenHolderIndex);
                if (finalTokenHolderIndex >= 0) {
                    topologyHelperLabel.setText("Server " + (finalTokenHolderIndex + 1)
                            + " dang giu token | Cam: server dang chon | Xanh/Do: online-offline");
                } else {
                    topologyHelperLabel.setText("Khong xac dinh duoc server dang giu token | Cam: server dang chon");
                }
            });
        }, "client-status-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private String requestStatus(int nodeIndex) {
        NodeEndpoint endpoint = endpoints[nodeIndex];
        try (Socket socket = new Socket(endpoint.host, endpoint.port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("STATUS");
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception ex) {
            return "LOI|" + ex.getMessage();
        }
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
        SwingUtilities.invokeLater(Client::new);
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
