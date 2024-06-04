package com.jrealm.game.graphics;

import lombok.Data;

@Data
public class Animation {

    private Sprite[] frames;
    private int[] states;
    private int currentFrame;
    private int numFrames;

    private int count;
    private int delay;

    private int timesPlayed;

    public Animation(Sprite[] frames) {
        this.setFrames(0, frames);
        this.timesPlayed = 0;
        this.states = new int[10];
    }

    public Animation() {
        this.timesPlayed = 0;
        this.states = new int[10];
    }

    public void setFrames(int state, Sprite[] frames) {
        this.frames = frames;
        this.currentFrame = 0;
        this.count = 0;
        this.timesPlayed = 0;
        this.delay = 2;
        if (this.states[state] == 0) {
            this.numFrames = frames.length;
        } else {
            this.numFrames = this.states[state];
        }
    }

    public void setDelay(int i) {
        this.delay = i;
    }

    public void setFrame(int i) {
        this.currentFrame = i;
    }

    public void setNumFrames(int i, int state) {
        this.states[state] = i;
    }

    public void update() {
        if (this.delay == -1)
            return;

        this.count++;

        if (this.count == this.delay) {
            this.currentFrame++;
            this.count = 0;
        }
        if (this.currentFrame == this.numFrames) {
            this.currentFrame = 0;
            this.timesPlayed++;
        }
    }

    public int getDelay() {
        return this.delay;
    }

    public int getFrame() {
        return this.currentFrame;
    }

    public int getCount() {
        return this.count;
    }

    public Sprite getImage() {
        if ((this.frames != null) && (this.frames.length == 1))
            return this.frames[0];
        return this.frames[this.currentFrame];
    }

    public boolean hasPlayedOnce() {
        return this.timesPlayed > 0;
    }

    public boolean hasPlayed(int i) {
        return this.timesPlayed == i;
    }

}
