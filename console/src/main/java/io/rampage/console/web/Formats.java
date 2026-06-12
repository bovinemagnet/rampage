package io.rampage.console.web;

import io.quarkus.qute.TemplateExtension;

import java.util.Locale;

/** Qute namespace extension — call as {@code {fmt:ms(value)}}, {@code {fmt:pct(value)}}, etc. */
@TemplateExtension(namespace = "fmt")
public class Formats {

    /**
     * Non-instantiable utility class; all members are static Qute extension methods.
     */
    public Formats() {}

    /**
     * Formats a millisecond value as a rounded integer with the {@code ms} suffix,
     * e.g. {@code "250 ms"}. Returns the em-dash {@code "—"} when {@code v} is {@code null}.
     *
     * @param v the value in milliseconds; may be {@code null}
     * @return the formatted string, never {@code null}
     */
    public static String ms(Double v) {
        return v == null ? "—" : Math.round(v) + " ms";
    }

    /**
     * Formats a percentage value to two decimal places, e.g. {@code "12.34%"}.
     * Returns the em-dash {@code "—"} when {@code v} is {@code null}.
     *
     * @param v the percentage value; may be {@code null}
     * @return the formatted string, never {@code null}
     */
    public static String pct(Double v) {
        return v == null ? "—" : String.format(Locale.ROOT, "%.2f%%", v);
    }

    /**
     * Converts an arbitrary value to its string representation via {@code String.valueOf}.
     * Returns the em-dash {@code "—"} when {@code v} is {@code null}.
     *
     * @param v the value to format; may be {@code null}
     * @return the string representation, never {@code null}
     */
    public static String num(Object v) {
        return v == null ? "—" : String.valueOf(v);
    }

    /**
     * Formats a signed, rounded delta, e.g. {@code "+30"}, {@code "-12"}, {@code "0"}.
     * Returns the em-dash {@code "—"} when {@code v} is {@code null}.
     *
     * @param v the delta value; may be {@code null}
     * @return the signed rounded string, never {@code null}
     */
    public static String signed(Double v) {
        if (v == null) {
            return "—";
        }
        long r = Math.round(v);
        return (r > 0 ? "+" : "") + r;
    }

    /**
     * Formats a signed percentage to one decimal place, e.g. {@code "+12.5%"}.
     * Returns the em-dash {@code "—"} when {@code v} is {@code null}.
     *
     * @param v the percentage value; may be {@code null}
     * @return the signed percentage string, never {@code null}
     */
    public static String signedPct(Double v) {
        if (v == null) {
            return "—";
        }
        return String.format(Locale.ROOT, "%+.1f%%", v);
    }
}
