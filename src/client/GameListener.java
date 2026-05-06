package client;

public interface GameListener {
    void onGameStart(String[] players, int[] cards);

    void onGameWinner(String winner, String expression);

    void onNoWinner();
}
