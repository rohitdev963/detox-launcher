package com.ansub.tvhome;
/** A contextual menu row keeps its action identity attached to its display label. */
final class SettingsChoice {
    final String id,label,value;
    SettingsChoice(String id,String label){this(id,label,"");}
    SettingsChoice(String id,String label,String value){this.id=id;this.label=label;this.value=value;}
}
