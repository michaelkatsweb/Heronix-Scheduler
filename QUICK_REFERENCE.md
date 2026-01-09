# Heronix Scheduling System - Quick Reference Card

**Version 2.0.0** | **Updated: Nov 5, 2025**

---

## 🚀 Quick Actions

### Fix Schedule Generation Issue
```
Tools → Clear Course Assignments... → OK → Generate New Schedule
```

### View Color Legend
```
Schedule Viewer Tab → Show Legend Button → See Colored Boxes
```

### Import Data
```
Import Wizard → Select File → Configure → Import
(Dialog: 1100x850 - All buttons visible)
```

### Generate Schedule
```
Schedules → Generate → Fill 3-Column Form → Generate
Left: Config | Center: Resources | Right: Progress
```

---

## 🎨 Theme Usage

### Apply Neumorphic Theme to FXML
```xml
<BorderPane stylesheets="@../css/neumorphic-theme.css"
            styleClass="neumorphic-card">
```

### Enable Dark Mode
```java
root.getStyleClass().add("dark-mode");
```

### Available Style Classes
```css
.neumorphic-card       /* Elevated card */
.neumorphic-surface    /* Raised element */
.neumorphic-inset      /* Pressed element */
.button-primary        /* Blue button */
.button-success        /* Green button */
.button-warning        /* Orange button */
.button-danger         /* Red button */
```

---

## 🎨 Color Palette

### Subject Colors
| Subject | Hex | Usage |
|---------|-----|-------|
| Math | `#3498DB` | Blue |
| Languages | `#27AE60` | Green |
| Social | `#F39C12` | Orange |
| Arts | `#9B59B6` | Purple |
| Physical Ed | `#E74C3C` | Red |
| Sciences | `#F1C40F` | Yellow |
| Breaks | `#95A5A6` | Gray |

### Accent Colors
| Purpose | Hex | Usage |
|---------|-----|-------|
| Primary | `#3498DB` | Main actions |
| Success | `#27AE60` | Confirmations |
| Warning | `#F39C12` | Cautions |
| Danger | `#E74C3C` | Deletions |
| Info | `#9B59B6` | Information |

---

## 📝 CSS Variables

### Light Mode
```css
-fx-base-background: #E4EBF5
-fx-text-primary: #2C3E50
-fx-text-secondary: #7F8C8D
```

### Dark Mode
```css
.dark-mode {
  -fx-base-background: #1E2127
  -fx-text-primary: #E4EBF5
  -fx-text-secondary: #A0A8B6
}
```

---

## 🔧 Troubleshooting

### Issue: Only One Teacher/Room Assigned
**Fix:** `Tools → Clear Course Assignments...`

### Issue: Theme Not Loading
**Fix:** Verify CSS path: `@../css/neumorphic-theme.css` in FXML `stylesheets` attribute

### Issue: CSS Parsing Error
**Fix:** Don't use CSS variables in inline `style=""` - use actual hex colors

### Issue: Import Dialog Buttons Cut Off
**Fix:** Update both FXML (1100x850) AND MainController dialog size code

### Issue: Dark Text on Dark Background
**Fix:** Add `stylesheets="@../css/neumorphic-theme.css"` and `styleClass="root"` to FXML

### Issue: Colors Don't Show in Legend
**Fix:** Use updated ScheduleViewerController (v2.0.0)

---

## 📊 Dialog Sizes

| Dialog | Old Size | New Size | Minimum |
|--------|----------|----------|---------|
| Import Wizard | 900x700 | 1100x850 | 1000x750 |
| AI Generator | 600x550 | 1400x850 | 1200x700 |
| Color Legend | Alert | 600x550 | - |

---

## 🗂️ Key Files

### Created
- `DatabaseMaintenanceService.java` - DB utility
- `neumorphic-theme.css` - Theme system
- `ScheduleGenerationDialog.fxml` - New layout
- `UI_IMPROVEMENTS_GUIDE.md` - Full docs
- `IMPROVEMENTS_SUMMARY.md` - Summary

### Modified
- `MainController.java` - Added clear handler, fixed dialog sizes
- `ScheduleViewerController.java` - Fixed legend with colored boxes
- `ScheduleGenerationDialogController.java` - Enhanced with resource counts
- `ImportWizard.fxml` - Increased size, added theme stylesheet
- `ScheduleGenerationDialog.fxml` - 3-column redesign, fixed CSS variables
- `Settings.fxml` - Added theme stylesheet
- `MainWindow.fxml` - Added menu item
- `CourseRepository.java` - Added findAllByCourseCodeIn() method

---

## 🎯 Testing Checklist

- [ ] Clear course assignments works (Tools menu)
- [ ] Color legend shows colored boxes (32x32px)
- [ ] Import wizard opens at 1100x850 with all buttons visible
- [ ] AI generator has 3 columns (Config | Resources | Progress)
- [ ] Resource counts populate in center column
- [ ] Schedule uses multiple teachers/rooms (after clearing assignments)
- [ ] Theme looks professional (neumorphic design)
- [ ] Settings page text is readable (not dark on dark)
- [ ] No CSS parsing errors in console
- [ ] All dialogs sized correctly

---

## 📞 Support

**Documentation:**
- `UI_IMPROVEMENTS_GUIDE.md` - Complete guide
- `IMPROVEMENTS_SUMMARY.md` - Detailed summary
- `QUICK_REFERENCE.md` - This file

**Status:** ✅ All improvements compiled successfully! (BUILD SUCCESS - 182 files, 0 errors)

---

## 💡 Pro Tips

1. **Always clear course assignments** before generating a new schedule (Tools → Clear...)
2. **Use diagnostic logs** to troubleshoot scheduling issues (check console output)
3. **Apply neumorphic-card** to containers for best appearance
4. **Don't use CSS variables in inline styles** - use actual hex colors instead
5. **Reference the color palette** for custom styling (see Color Palette section)
6. **Test dialog sizes** in both FXML and controller code
7. **Add stylesheets attribute** to FXML root for theme support
8. **Check for CSS parsing errors** in console during development

---

**Happy Scheduling!** 🎓📚
