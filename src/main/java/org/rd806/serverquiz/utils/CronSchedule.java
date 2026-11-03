package org.rd806.serverquiz.utils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 极简 cron 表达式解析器。
 * <p>
 * 支持 5 段（分 时 日 月 周）和 6 段（秒 分 时 日 月 周）表达式，字段取值：
 * <ul>
 *     <li>秒、分：0-59</li>
 *     <li>时：0-23</li>
 *     <li>日：1-31</li>
 *     <li>月：1-12</li>
 *     <li>周：0 或 7 表示周日，1-6 表示周一到周六</li>
 * </ul>
 * 每个字段支持：星号（任意值）、单个数字、a-b 范围、步长（a-b/n 或 a/n），以及逗号分隔的列表。
 * 未提供秒字段时，表达式在该分钟内的任意时刻匹配一次即可触发。
 */
public class CronSchedule {

    private final Set<Integer> seconds;
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> daysOfMonth;
    private final Set<Integer> months;
    private final Set<Integer> daysOfWeek;
    private final boolean hasSeconds;
    private final boolean dayOfMonthRestricted;
    private final boolean dayOfWeekRestricted;

    private CronSchedule(Set<Integer> seconds, Set<Integer> minutes, Set<Integer> hours,
                         Set<Integer> daysOfMonth, Set<Integer> months, Set<Integer> daysOfWeek,
                         boolean hasSeconds, boolean dayOfMonthRestricted, boolean dayOfWeekRestricted) {
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
        this.hasSeconds = hasSeconds;
        this.dayOfMonthRestricted = dayOfMonthRestricted;
        this.dayOfWeekRestricted = dayOfWeekRestricted;
    }

    /**
     * 解析 cron 表达式，格式错误时抛出 IllegalArgumentException
     */
    public static CronSchedule parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("cron expression is empty");
        }

        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 5 && parts.length != 6) {
            throw new IllegalArgumentException("expected 5 or 6 fields, got " + parts.length);
        }

        boolean hasSeconds = parts.length == 6;
        int index = 0;

        Set<Integer> seconds = hasSeconds ? parseField(parts[index++], 0, 59, false) : Set.of(0);
        Set<Integer> minutes = parseField(parts[index++], 0, 59, false);
        Set<Integer> hours = parseField(parts[index++], 0, 23, false);
        Set<Integer> daysOfMonth = parseField(parts[index++], 1, 31, false);
        Set<Integer> months = parseField(parts[index++], 1, 12, false);
        Set<Integer> daysOfWeek = parseField(parts[index], 0, 7, true);

        return new CronSchedule(seconds, minutes, hours, daysOfMonth, months, daysOfWeek,
                hasSeconds, !isWildcard(parts[parts.length - 3]), !isWildcard(parts[parts.length - 1]));
    }

    /**
     * 判断给定时间是否匹配表达式。
     * <p>
     * 未提供秒字段时只比较到分钟；日字段和周字段同时被限定时，按 cron 惯例取“或”。
     */
    public boolean matches(LocalDateTime time) {
        if (hasSeconds && !seconds.contains(time.getSecond())) return false;
        if (!minutes.contains(time.getMinute())) return false;
        if (!hours.contains(time.getHour())) return false;
        if (!months.contains(time.getMonthValue())) return false;

        boolean dayOfMonthMatch = daysOfMonth.contains(time.getDayOfMonth());
        // DayOfWeek: MONDAY=1 ... SUNDAY=7，转换为 cron 的 SUNDAY=0
        int dayOfWeek = time.getDayOfWeek().getValue() % 7;
        boolean dayOfWeekMatch = daysOfWeek.contains(dayOfWeek);

        if (dayOfMonthRestricted && dayOfWeekRestricted) {
            return dayOfMonthMatch || dayOfWeekMatch;
        }
        if (dayOfMonthRestricted) return dayOfMonthMatch;
        if (dayOfWeekRestricted) return dayOfWeekMatch;
        return true;
    }

    // 表达式是否包含秒字段
    public boolean hasSeconds() {
        return hasSeconds;
    }

    // 字段是否覆盖全部取值
    private static boolean isWildcard(String field) {
        return field.equals("*") || field.equals("*/1");
    }

    // 解析单个字段
    private static Set<Integer> parseField(String field, int min, int max, boolean dayOfWeek) {
        Set<Integer> values = new TreeSet<>();
        for (String part : field.split(",")) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("empty item in field \"" + field + "\"");
            }

            int step = 1;
            String range = part;
            int slash = part.indexOf('/');
            if (slash >= 0) {
                range = part.substring(0, slash);
                step = parseInt(part.substring(slash + 1), part);
                if (step <= 0) {
                    throw new IllegalArgumentException("step must be positive: " + part);
                }
            }

            int start;
            int end;
            if (range.equals("*")) {
                start = min;
                end = max;
            } else if (range.contains("-")) {
                String[] bounds = range.split("-", 2);
                start = parseInt(bounds[0], part);
                end = parseInt(bounds[1], part);
            } else {
                start = parseInt(range, part);
                // "5/2" 表示从 5 开始按步长一直取到上限
                end = slash >= 0 ? max : start;
            }

            if (start < min || end > max || start > end) {
                throw new IllegalArgumentException("value out of range in \"" + part + "\" (allowed " + min + "-" + max + ")");
            }
            for (int value = start; value <= end; value += step) {
                values.add(dayOfWeek && value == 7 ? 0 : value);
            }
        }
        return values;
    }

    // 解析整数，失败时给出可读错误
    private static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid value in \"" + field + "\"");
        }
    }
}
