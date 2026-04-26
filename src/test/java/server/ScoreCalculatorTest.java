package server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreCalculatorTest {
    @Test
    void shouldReturnZeroWhenAnswerIsWrong() {
        assertEquals(0, ScoreCalculator.calculatePoints(false, 1000));
    }

    @Test
    void shouldReturn150WhenCorrectUnderThreeSeconds() {
        assertEquals(150, ScoreCalculator.calculatePoints(true, 2000));
    }

    @Test
    void shouldReturn130WhenCorrectBetweenThreeAndSevenSeconds() {
        assertEquals(130, ScoreCalculator.calculatePoints(true, 5000));
        assertEquals(130, ScoreCalculator.calculatePoints(true, 7000));
    }

    @Test
    void shouldReturn110WhenCorrectBetweenSevenAndTwelveSeconds() {
        assertEquals(110, ScoreCalculator.calculatePoints(true, 9000));
        assertEquals(110, ScoreCalculator.calculatePoints(true, 12000));
    }

    @Test
    void shouldReturn100WhenCorrectAfterTwelveSeconds() {
        assertEquals(100, ScoreCalculator.calculatePoints(true, 13000));
    }
}
