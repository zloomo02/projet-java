package server;

public final class ScoreCalculator {
    private ScoreCalculator() {
    }

    /**
     * Calcule les points selon les regles du quiz (base + bonus rapidite).
     */
    public static int calculatePoints(boolean correct, long elapsedMillis) {
        if (!correct) {
            return 0;
        }

        int basePoints = 100;
        double elapsedSeconds = Math.max(0, elapsedMillis) / 1000.0;

        int speedBonus;
        if (elapsedSeconds < 3.0) {
            speedBonus = 50;
        } else if (elapsedSeconds <= 7.0) {
            speedBonus = 30;
        } else if (elapsedSeconds <= 12.0) {
            speedBonus = 10;
        } else {
            speedBonus = 0;
        }

        return basePoints + speedBonus;
    }
}
