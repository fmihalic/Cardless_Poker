package com.mihalic.franck.cardless_poker;

/**
 * Created by fmihalic on 23/04/2017.
 */

public class Player {

    private int number;
    private boolean active;

    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}
