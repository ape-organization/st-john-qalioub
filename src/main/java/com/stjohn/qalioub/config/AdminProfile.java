package com.stjohn.qalioub.config;

public enum AdminProfile {

    MICHAEL_HANNA("مايكل عبدالمسيح", "201203813184", "https://ipn.eg/S/michael_hanna2006/instapay/1wZWxu"),
    PETER_MAHER("بيتر ماهر", "201070382811", "https://ipn.eg/S/petermaher112/instapay/7C3vTm"),
    MICHAEL_SAAD("مايكل سعد", "201091587701", "https://ipn.eg/S/m1kllz/instapay/0TvOes"),
    GERGES_MOUSA("جرجس موسى", "201068047342", "https://ipn.eg/S/gergesmousa-209/instapay/9ELioF");

    private final String displayName;
    private final String whatsappPhone;
    private final String instapayLink;

    AdminProfile(String displayName, String whatsappPhone, String instapayLink) {
        this.displayName = displayName;
        this.whatsappPhone = whatsappPhone;
        this.instapayLink = instapayLink;
    }

    public String getDisplayName() { return displayName; }
    public String getWhatsappPhone() { return whatsappPhone; }
    public String getInstapayLink() { return instapayLink; }
}
