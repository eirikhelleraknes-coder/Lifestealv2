package com.lifesteal.config;

public class LifestealConfig {
    public int starting_hearts = 10;
    public int max_hearts = 20; // 0 = infinite
    public int combat_timer_seconds = 60;
    public int dummy_duration_seconds = 30;
    public boolean bonus_hearts_on_kill = false; // award extra hearts based on victim's kill streak
    public boolean use_dummy_on_clog = true; // if false, instantly kill the player on combat log instead of spawning a dummy
}
