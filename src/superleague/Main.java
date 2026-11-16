package superleague;

import java.awt.GraphicsEnvironment;
import java.util.Scanner;
import superleague.gui.GameFrame;
import superleague.ui.ConsoleUI;

public class Main {
    public static void main(String[] args) throws java.io.IOException {
        long seed = System.currentTimeMillis();
        boolean auto = false;
        boolean console = false;
        int autoTeam = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--auto" -> auto = true;
                case "--console" -> console = true;
                case "--team" -> autoTeam = Integer.parseInt(args[++i]) - 1;
                case "--web" -> {
                    superleague.web.WebServer.main(new String[0]);
                    return;
                }
                default -> {
                }
            }
        }

        if (!console && !auto && !GraphicsEnvironment.isHeadless()) {
            GameFrame.launch();
            return;
        }
        new ConsoleUI(new Scanner(System.in), seed, auto, autoTeam).run();
    }
}
