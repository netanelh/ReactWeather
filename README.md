# Vortex Client — Minecraft 26.2 (Fabric)

קליינט לשרת הפרטי ולסינגל פלייר. **Right Shift** פותח את ה-ClickGUI.

## בנייה והרצה

דרישות: Java 25 (אותה סביבה כמו Lyrics Plus).

```bash
./gradlew runClient     # מריץ מיינקראפט עם הקליינט, לבדיקה
./gradlew build         # בונה jar ל-build/libs/vortex-client-1.0.0.jar
```

ה-jar נכנס לתיקיית `mods`, יחד עם Fabric API לגרסה 26.2.

## הפעלה במשחק

| פעולה | תוצאה |
|---|---|
| קליק שמאלי על מודול | הדלקה / כיבוי |
| קליק ימני על מודול | פתיחת ההגדרות |
| קליק אמצעי על מודול | הגדרת מקש (Esc מנקה) |
| גרירת כותרת פאנל | הזזת הפאנל |
| קליק ימני על כותרת | כיווץ הפאנל |
| גלגלת | גלילת פאנלים |
| הקלדה | חיפוש מודולים |
| קליק ימני על סליידר | איפוס לברירת המחדל |

ההגדרות נשמרות אוטומטית בקובץ `config/vortex.json`.

## מודולים

- **Combat:** KillAura (עם Predict), TriggerBot, AimAssist, BowAimbot, AutoClicker, Criticals, Reach, Hitboxes, Velocity, AutoTotem, AutoArmor, AutoWeapon, Surround, AutoTrap, AutoPearl, AutoGap
- **Movement:** Flight, ElytraFly, BoatFly, Speed, LongJump, Sprint, NoFall, Step, Spider, Jesus, AirJump, HighJump, SafeWalk, Parkour, AutoWalk, Blink
- **Render:** Fullbright, Xray, ESP, NoWeather, Radar, TargetHUD, DangerAlert
- **Player:** AutoEat, AutoTool, ChestStealer, AutoRespawn, AntiAFK, AutoFish, FastPlace, FastBreak, AutoReplenish, InvCleaner, AutoMend, MiddleClickPearl
- **World:** Nuker, Scaffold, AutoFarm
- **Pathing:** Navigator, AutoMine, FarmBot, Follow, Explore, Tunnel
- **Client:** ClickGUI, HUD, Panic

## פקודות ניווט (בצ'אט)

```
.goto x y z | .goto x z | .goto <waypoint>   .home  .sethome  .stop  .pause  .resume
.mine diamond iron [limit 20] [nohome]       .mine ores
.follow [player]   .explore   .farm [radius]   .tunnel [length] [1x2|2x2|3x3]
.wp add|del|list <name>       (נקודת מוות נשמרת אוטומטית)
.toggle <module>  .bind <module> <key>  .preset save|load|list <name>  .panic
```

## אם משהו לא עובד

- **שגיאת קומפילציה:** הקוד נכתב מול ה-API של 26.2 אבל לא קומפל בפועל. צריך להעתיק את השגיאה המלאה.
- **מודול שלא עושה כלום:** ה-mixins מוגדרים עם `defaultRequire: 0`, אז hook שלא מצא את המתודה שלו לא מקריס את המשחק אלא רק כותב אזהרה. צריך לחפש בלוג (`logs/latest.log`) שורות עם `vortex` ו-`could not find`.
- **Xray לא עובד עם Sodium:** זה ידוע. Sodium מחליף את מנוע רינדור הבלוקים, ולכן כרגע צריך להריץ בלעדיו.
