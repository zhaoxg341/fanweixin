package cn.itcast.weixin.utils;

import android.content.Context;
import android.content.SharedPreferences;

import cn.itcast.weixin.WeixinApp;

/**
 * 小游戏战绩，存在 SharedPreferences 里，退出 App 也不会丢。
 *
 * 只记「值得看」的三项：
 * - 猜拳：胜 / 负场数
 * - 猜数字：猜中时最少用了几次
 * - 翻牌：通关时最少用了几步（步数相同再看用时？这里只留步数，规则简单）
 *
 * 另外记下每种玩法的完成次数，方便以后扩展成就，界面上暂时不展示。
 */
public class GameStats {

    private static final String PREF = "game_stats";

    private static final String KEY_RPS_WIN = "rps_win";
    private static final String KEY_RPS_LOSE = "rps_lose";
    private static final String KEY_RPS_DRAW = "rps_draw";

    private static final String KEY_GUESS_WIN = "guess_win";
    /** 最少几次猜中，0 表示还没有记录 */
    private static final String KEY_GUESS_BEST = "guess_best";

    private static final String KEY_MEMORY_WIN = "memory_win";
    /** 最少几步通关，0 表示还没有记录 */
    private static final String KEY_MEMORY_BEST = "memory_best";

    private GameStats() {
    }

    private static SharedPreferences prefs() {
        return WeixinApp.getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** 猜拳结果：1 我赢、0 平、-1 我输 */
    public static void addRps(int result) {
        SharedPreferences p = prefs();
        SharedPreferences.Editor editor = p.edit();
        if (result > 0) {
            editor.putInt(KEY_RPS_WIN, p.getInt(KEY_RPS_WIN, 0) + 1);
        } else if (result < 0) {
            editor.putInt(KEY_RPS_LOSE, p.getInt(KEY_RPS_LOSE, 0) + 1);
        } else {
            editor.putInt(KEY_RPS_DRAW, p.getInt(KEY_RPS_DRAW, 0) + 1);
        }
        editor.apply();
    }

    /** 猜数字猜中，used 是第几次猜中（越少越好） */
    public static void addGuessWin(int used) {
        if (used <= 0) {
            return;
        }
        SharedPreferences p = prefs();
        SharedPreferences.Editor editor = p.edit();
        editor.putInt(KEY_GUESS_WIN, p.getInt(KEY_GUESS_WIN, 0) + 1);
        int best = p.getInt(KEY_GUESS_BEST, 0);
        if (best == 0 || used < best) {
            editor.putInt(KEY_GUESS_BEST, used);
        }
        editor.apply();
    }

    /**
     * 翻牌通关。
     *
     * @return 是不是刷新了最少步数纪录（用来在界面上弹一句「新纪录」）
     */
    public static boolean addMemoryWin(int steps) {
        if (steps <= 0) {
            return false;
        }
        SharedPreferences p = prefs();
        SharedPreferences.Editor editor = p.edit();
        editor.putInt(KEY_MEMORY_WIN, p.getInt(KEY_MEMORY_WIN, 0) + 1);
        int best = p.getInt(KEY_MEMORY_BEST, 0);
        boolean record = best == 0 || steps < best;
        if (record) {
            editor.putInt(KEY_MEMORY_BEST, steps);
        }
        editor.apply();
        return record;
    }

    public static int rpsWin() {
        return prefs().getInt(KEY_RPS_WIN, 0);
    }

    public static int rpsLose() {
        return prefs().getInt(KEY_RPS_LOSE, 0);
    }

    public static int rpsDraw() {
        return prefs().getInt(KEY_RPS_DRAW, 0);
    }

    public static int guessBest() {
        return prefs().getInt(KEY_GUESS_BEST, 0);
    }

    public static int memoryBest() {
        return prefs().getInt(KEY_MEMORY_BEST, 0);
    }

    /** 有没有任何一条战绩，游戏大厅用它决定显示数字还是「—」 */
    public static boolean hasAny() {
        return rpsWin() + rpsLose() + rpsDraw() > 0
                || guessBest() > 0 || memoryBest() > 0;
    }

    public static void clear() {
        prefs().edit().clear().apply();
    }
}
