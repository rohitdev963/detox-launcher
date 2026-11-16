package com.ansub.tvhome;
import java.util.HashSet;
/** Host-JVM regression checks for newly integrated Home themes and clock selection. */
public final class HomeClockThemeRegressionTest {
 public static void main(String[] args) {
  if (ClockFacePolicy.KEYS.length != 17 || ClockFacePolicy.NAMES.length != 17) throw new AssertionError("clock catalog count");
  String[] retired = {null,"unknown","station","classicwall","romandress","skeleton","crystal","moonphase","gothic","anime","wizarding","gaming","superhero","minimalroman","glasshalo","minimalarabic","depthdigital","digital24","digital"};
  for (String key : retired) {
   String migrated = ClockFacePolicy.normalise(key);
   if (!"digital".equals(migrated) || ClockFacePolicy.analog(key)) throw new AssertionError("retired style migration: " + key);
   if (!migrated.equals(ClockFacePolicy.normalise(migrated))) throw new AssertionError("migration not idempotent");
  }
  int count = HomePremiumThemes.IDS.length;
  if (count != 7 || HomePremiumThemes.NAMES.length != count || HomePremiumThemes.ACCENTS.length != count || HomePremiumThemes.SURFACES.length != count || HomePremiumThemes.BORDERS.length != count || HomePremiumThemes.RADII.length != count || HomePremiumThemes.CLOCK_PRIMARY.length != count || HomePremiumThemes.CLOCK_SECONDARY.length != count || HomePremiumThemes.CLOCK_FONTS.length != count || HomePremiumThemes.CLOCK_SPACING.length != count) throw new AssertionError("theme array mismatch");
  HashSet<String> themes = new HashSet<>();
  for (int i=0; i<count; i++) {
   if (!themes.add(HomePremiumThemes.IDS[i]) || HomePremiumThemes.index(HomePremiumThemes.IDS[i]) != i) throw new AssertionError("theme mapping");
   if (HomePremiumThemes.tileGradient(i).length != 2 || HomePremiumThemes.focusGradient(i).length != 3) throw new AssertionError("gradient stops");
  }
  if (HomePremiumThemes.index(null) != 0 || HomePremiumThemes.index("unknown") != 0 || HomePremiumThemes.tileGradient(-5)[0] != 0) throw new AssertionError("theme fallback");
  System.out.println("Home clock/theme regression: digital fallback, 7 themes, retired-face migrations and gradients passed");
 }
}
