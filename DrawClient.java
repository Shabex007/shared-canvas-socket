import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class DrawClient {
    private static final int PORT = 5000;
    private static final String HOST = "localhost";

    public static void main(String[] args) throws Exception {
        new DrawClient().start();
    }

    private void start() throws IOException {
        Socket socket = new Socket(HOST, PORT);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        JFrame frame = new JFrame("Shared Drawing Canvas");
        DrawPanel drawPanel = new DrawPanel(out);

        JPanel controls = new JPanel();
        JTextField usernameField = new JTextField("User", 10);
        JButton clearBtn = new JButton("Clear");
        JComboBox<String> colorBox = new JComboBox<>(new String[]{"Black", "Red", "Green", "Blue"});
        JCheckBox eraserCheck = new JCheckBox("Eraser");

        controls.add(new JLabel("Username:"));
        controls.add(usernameField);
        controls.add(new JLabel("Color:"));
        controls.add(colorBox);
        controls.add(eraserCheck);
        controls.add(clearBtn);

        clearBtn.addActionListener(e -> drawPanel.sendClear());
        colorBox.addActionListener(e -> drawPanel.setColor(getColorFromName((String) colorBox.getSelectedItem())));
        eraserCheck.addActionListener(e -> drawPanel.setEraser(eraserCheck.isSelected()));
        usernameField.addActionListener(e -> drawPanel.setUsername(usernameField.getText()));

        drawPanel.setColor(getColorFromName((String) colorBox.getSelectedItem()));
        drawPanel.setUsername(usernameField.getText());

        frame.setLayout(new BorderLayout());
        frame.add(drawPanel, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.NORTH);
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(() -> {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    if (obj instanceof DrawCommand cmd) {
                        drawPanel.processCommand(cmd);
                    }
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server.");
            }
        }).start();
    }

    private static Color getColorFromName(String name) {
        return switch (name) {
            case "Red" -> Color.RED;
            case "Green" -> Color.GREEN;
            case "Blue" -> Color.BLUE;
            default -> Color.BLACK;
        };
    }

    static class DrawPanel extends JPanel {
        private final ArrayList<DrawCommand> commands = new ArrayList<>();
        private final ObjectOutputStream out;
        private Color currentColor = Color.BLACK;
        private String username = "User";
        private Point lastPoint = null;
        private boolean eraserMode = false;

        public DrawPanel(ObjectOutputStream out) {
            this.out = out;
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastPoint = e.getPoint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    Point current = e.getPoint();
                    Color drawColor = eraserMode ? Color.WHITE : currentColor;
                    DrawCommand cmd = new DrawCommand(lastPoint, current, drawColor, username, false);
                    lastPoint = current;
                    commands.add(cmd);
                    repaint();
                    try {
                        out.writeObject(cmd);
                        out.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        public void setColor(Color color) {
            this.currentColor = color;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setEraser(boolean isEraser) {
            this.eraserMode = isEraser;
        }

        public void sendClear() {
            DrawCommand clearCmd = new DrawCommand(null, null, Color.WHITE, username, true);
            try {
                out.writeObject(clearCmd);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void processCommand(DrawCommand cmd) {
            if (cmd.clear) {
                commands.clear();
            } else {
                commands.add(cmd);
            }
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (DrawCommand cmd : commands) {
                if (cmd.start != null && cmd.end != null) {
                    g.setColor(cmd.color);
                    ((Graphics2D) g).setStroke(new BasicStroke(2));
                    g.drawLine(cmd.start.x, cmd.start.y, cmd.end.x, cmd.end.y);
                }
            }
        }
    }
}