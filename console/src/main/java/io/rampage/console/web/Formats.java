package io.rampage.console.web;

import io.quarkus.qute.TemplateExtension;

import java.util.Locale;

/** Qute namespace extension — call as {@code {fmt:ms(value)}}, {@code {fmt:pct(value)}}, etc. */
@TemplateExtension(namespace = "fmt")
public class Formats {

    public static String ms(Double v) {
        return v == null ? "—" : Math.round(v) + " ms";
    }

    public static String pct(Double v) {
        return v == null ? "—" : String.format(Locale.ROOT, "%.2f%%", v);
    }

    public static String num(Object v) {
        return v == null ? "—" : String.valueOf(v);
    }

    /** Signed, rounded delta — "+30", "-12", "0". */
    public static String signed(Double v) {
        if (v == null) {
            return "—";
        }
        long r = Math.round(v);
        return (r > 0 ? "+" : "") + r;
    }

    /** Signed percentage to one decimal — "+12.5%". */
    public static String signedPct(Double v) {
        if (v == null) {
            return "—";
        }
        return String.format(Locale.ROOT, "%+.1f%%", v);
    }
}
