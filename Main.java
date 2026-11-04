package game;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = GameFrame.getInstance();
            frame.showScreen(GameFrame.MENU);
            frame.setVisible(true);
        });
    }
}
