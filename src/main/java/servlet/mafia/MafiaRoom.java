package servlet.mafia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;



public class MafiaRoom {
    private static final int FIELD_SIZE = 25;
    private volatile int firstPlayer;
    private volatile int roomId;
    private volatile int secondPlayer = -1;
    private volatile int firstCharacter;
    private volatile int secondCharacter;
    private volatile ArrayList<Integer> field = new ArrayList<>();
    private volatile AsyncWrapper firstPlayerContext;

    public MafiaRoom(int firstPlayer, int roomId) {
        this.firstPlayer = firstPlayer;
        this.roomId = roomId;

        for(int i = 0; i < FIELD_SIZE; i++) {
            field.add(i + 1);
        }
        Collections.shuffle(field);
        firstCharacter = (ThreadLocalRandom.current().nextInt() % (FIELD_SIZE)) + 1;
        firstCharacter = Math.abs(firstCharacter);
        secondCharacter = firstCharacter;
        while(secondCharacter == firstCharacter) {
            secondCharacter = (ThreadLocalRandom.current().nextInt() % (FIELD_SIZE)) + 1;
            secondCharacter = Math.abs(secondCharacter);
        }
    }

    synchronized public void setFirstAsyncContext(AsyncWrapper context) {
        this.firstPlayerContext = context;
    }

    synchronized public AsyncWrapper getFirstPlayerContext() {
        return firstPlayerContext;
    }

    synchronized public void setSecondPlayer(int secondPlayer) {
        this.secondPlayer = secondPlayer;
    }

    synchronized public int getFirstPlayer() {
        return firstPlayer;
    }

    synchronized public int getSecondPlayer() {
        return secondPlayer;
    }

    synchronized public int getFirstCharacter() {
        return firstCharacter;
    }

    synchronized public int getSecondCharacter() {
        return secondCharacter;
    }

    synchronized public int getRoomId() {
        return roomId;
    }

    synchronized public List<Integer> getField() {
        return field;
    }
}
