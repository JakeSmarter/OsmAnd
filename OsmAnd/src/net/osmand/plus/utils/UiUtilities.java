package net.osmand.plus.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TintableCompoundButton;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.SnackbarContentLayout;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import gnu.trove.map.hash.TLongObjectHashMap;

public class UiUtilities {

	private static final Log LOG = PlatformUtil.getLog(UiUtilities.class);

	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;
	private final TLongObjectHashMap<Drawable> drawableCache = new TLongObjectHashMap<>();
	private final OsmandApplication app;
	private static final int INVALID_ID = -1;

	public enum DialogButtonType {
		PRIMARY,
		PRIMARY_HARMFUL,
		SECONDARY,
		SECONDARY_HARMFUL,
		SECONDARY_ACTIVE,
		STROKED
	}

	public enum CompoundButtonType {
		GLOBAL,
		PROFILE_DEPENDENT,
		TOOLBAR
	}

	public enum CustomRadioButtonType {
		START,
		CENTER,
		END,
	}

	public UiUtilities(OsmandApplication app) {
		this.app = app;
	}

	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId) {
		long hash = ((long) resId << 31l) + clrId;
		Drawable d = drawableCache.get(hash);
		if (d == null) {
			d = AppCompatResources.getDrawable(app, resId);
			d = DrawableCompat.wrap(d);
			d.mutate();
			if (clrId != 0) {
				DrawableCompat.setTint(d, ContextCompat.getColor(app, clrId));
			}
			drawableCache.put(hash, d);
		}
		return d;
	}

	@Nullable
	private Drawable getPaintedDrawable(@DrawableRes int resId, @ColorInt int color) {
		Drawable drawable = null;
		if (resId != 0) {
			long hash = ((long) resId << 31L) + color;
			drawable = drawableCache.get(hash);
			if (drawable == null) {
				drawable = AppCompatResources.getDrawable(app, resId);
				drawable = tintDrawable(drawable, color);

				drawableCache.put(hash, drawable);
			}
		} else {
			LOG.warn("Invalid icon identifier");
		}
		return drawable;
	}

	@Nullable
	public Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return getPaintedDrawable(id, color);
	}

	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getDrawable(id, colorId);
	}

	public Drawable getLayeredIcon(@DrawableRes int bgIconId, @DrawableRes int foregroundIconId) {
		return getLayeredIcon(bgIconId, foregroundIconId, 0, 0);
	}

	public Drawable getLayeredIcon(@DrawableRes int bgIconId, @DrawableRes int foregroundIconId,
	                               @ColorRes int bgColorId, @ColorRes int foregroundColorId) {
		Drawable background = getDrawable(bgIconId, bgColorId);
		Drawable foreground = getDrawable(foregroundIconId, foregroundColorId);
		return getLayeredIcon(background, foreground);
	}

	public static Drawable getLayeredIcon(Drawable... icons) {
		return new LayerDrawable(icons);
	}

	public Drawable getThemedIcon(@DrawableRes int id) {
		return getDrawable(id, R.color.icon_color_default_light);
	}

	public Drawable getIcon(@DrawableRes int id) {
		return getDrawable(id, 0);
	}

	public Drawable getIcon(@DrawableRes int id, boolean light) {
		return getDrawable(id, ColorUtilities.getDefaultIconColorId(!light));
	}

	public static Drawable getColoredSelectableDrawable(Context ctx, int color, float alpha) {
		int colorWithAlpha = ColorUtilities.getColorWithAlpha(color, alpha);
		return getColoredSelectableDrawable(ctx, colorWithAlpha);
	}

	public static Drawable getColoredSelectableDrawable(Context ctx, int color) {
		Drawable drawable = null;
		Drawable bg = getSelectableDrawable(ctx);
		if (bg != null) {
			drawable = tintDrawable(bg, color);
		}
		return drawable;
	}

	public static Drawable getSelectableDrawable(Context ctx) {
		int bgResId = AndroidUtils.resolveAttribute(ctx, R.attr.selectableItemBackground);
		if (bgResId != 0) {
			return AppCompatResources.getDrawable(ctx, bgResId);
		}
		return null;
	}

	public static Drawable createTintedDrawable(Context context, @DrawableRes int resId, @ColorInt int color) {
		return tintDrawable(AppCompatResources.getDrawable(context, resId), color);
	}

	public static Drawable tintDrawable(Drawable drawable, @ColorInt int color) {
		Drawable coloredDrawable = null;
		if (drawable != null) {
			coloredDrawable = DrawableCompat.wrap(drawable);
			if (coloredDrawable.getConstantState() != null) {
				coloredDrawable = coloredDrawable.getConstantState().newDrawable();
			}
			coloredDrawable.mutate();
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && coloredDrawable instanceof RippleDrawable) {
				((RippleDrawable) coloredDrawable).setColor(ColorStateList.valueOf(color));
			} else {
				DrawableCompat.setTint(coloredDrawable, color);
			}
		}

		return coloredDrawable;
	}

	public UpdateLocationViewCache getUpdateLocationViewCache() {
		return getUpdateLocationViewCache(true);
	}

	public UpdateLocationViewCache getUpdateLocationViewCache(boolean useScreenOrientation) {
		UpdateLocationViewCache uvc = new UpdateLocationViewCache();
		if (useScreenOrientation) {
			uvc.screenOrientation = getScreenOrientation();
		}
		return uvc;
	}

	public static class UpdateLocationViewCache {
		int screenOrientation;
		public boolean paintTxt = true;
		public int arrowResId;
		public int arrowColor;
		public int textColor;
		public LatLon specialFrom;
	}

	public void updateLocationView(UpdateLocationViewCache cache, ImageView arrow, TextView txt,
			double toLat, double toLon) {
		updateLocationView(cache, arrow, txt, new LatLon(toLat, toLon));
	}

	public void updateLocationView(UpdateLocationViewCache cache, ImageView arrow, TextView txt,
			LatLon toLoc) {
		float[] mes = new float[2];
		boolean stale = false;
		LatLon fromLoc = cache == null ? null : cache.specialFrom;
		boolean useCenter = fromLoc != null;
		Float h = null;
		if (fromLoc == null) {
			Location loc = app.getLocationProvider().getLastKnownLocation();
			h = app.getLocationProvider().getHeading();
			if (loc == null) {
				loc = app.getLocationProvider().getLastStaleKnownLocation();
				stale = true;
			}
			if (loc != null) {
				fromLoc = new LatLon(loc.getLatitude(), loc.getLongitude());
			} else {
				useCenter = true;
				stale = false;
				fromLoc = app.getMapViewTrackingUtilities().getMapLocation();
				h = app.getMapViewTrackingUtilities().getMapRotate();
				if (h != null) {
					h = -h;
				}
			}
		}
		if (fromLoc != null && toLoc != null) {
			Location.distanceBetween(toLoc.getLatitude(), toLoc.getLongitude(), fromLoc.getLatitude(),
					fromLoc.getLongitude(), mes);
		}

		if (arrow != null) {
			boolean newImage = false;
			int arrowResId = cache == null ? 0 : cache.arrowResId;
			if (arrowResId == 0) {
				arrowResId = R.drawable.ic_direction_arrow;
			}
			DirectionDrawable dd;
			if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
				newImage = true;
				dd = new DirectionDrawable(app, arrow.getWidth(), arrow.getHeight());
			} else {
				dd = (DirectionDrawable) arrow.getDrawable();
			}
			int imgColorSet = cache == null ? 0 : cache.arrowColor;
			if (imgColorSet == 0) {
				imgColorSet = useCenter ? R.color.color_distance : R.color.color_myloc_distance;
				if (stale) {
					imgColorSet = R.color.icon_color_default_light;
				}
			}
			dd.setImage(arrowResId, imgColorSet);
			if (fromLoc == null || h == null || toLoc == null) {
				dd.setAngle(0);
			} else {
				float orientation = (cache == null ? 0 : cache.screenOrientation);
				dd.setAngle(mes[1] - h + 180 + orientation);
			}
			if (newImage) {
				arrow.setImageDrawable(dd);
			}
			arrow.invalidate();
		}
		if (txt != null) {
			if (fromLoc != null && toLoc != null) {
				if (cache.paintTxt) {
					int textColorSet = cache.textColor;
					if (textColorSet == 0) {
						textColorSet = useCenter ? R.color.color_distance : R.color.color_myloc_distance;
						if (stale) {
							textColorSet = R.color.icon_color_default_light;
						}
					}
					txt.setTextColor(ColorUtilities.getColor(app, textColorSet));
				}
				txt.setText(OsmAndFormatter.getFormattedDistance(mes[0], app));
			} else {
				txt.setText("");
			}
		}
	}

	public int getScreenOrientation() {
		int screenOrientation = ((WindowManager) app.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		switch (screenOrientation) {
			case ORIENTATION_0:   // Device default (normally portrait)
				screenOrientation = 0;
				break;
			case ORIENTATION_90:  // Landscape right
				screenOrientation = 90;
				break;
			case ORIENTATION_270: // Landscape left
				screenOrientation = 270;
				break;
			case ORIENTATION_180: // Upside down
				screenOrientation = 180;
				break;
		}
		//Looks like screenOrientation correction must not be applied for devices without compass?
		PackageManager manager = app.getPackageManager();
		boolean hasCompass = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
		if (!hasCompass) {
			screenOrientation = 0;
		}
		return screenOrientation;
	}

	public static void setupSnackbar(Snackbar snackbar, boolean nightMode) {
		setupSnackbar(snackbar, nightMode, null, null, null, null);
	}

	public static void setupSnackbar(Snackbar snackbar, boolean nightMode, Integer maxLines) {
		setupSnackbar(snackbar, nightMode, null, null, null, maxLines);
	}

	public static void setupSnackbar(Snackbar snackbar, boolean nightMode, @ColorRes Integer backgroundColor,
	                                 @ColorRes Integer messageColor, @ColorRes Integer actionColor, Integer maxLines) {
		if (snackbar == null) {
			return;
		}
		View view = snackbar.getView();
		Context ctx = view.getContext();
		TextView tvMessage = view.findViewById(com.google.android.material.R.id.snackbar_text);
		TextView tvAction = view.findViewById(com.google.android.material.R.id.snackbar_action);
		if (messageColor == null) {
			messageColor = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		}
		tvMessage.setTextColor(ContextCompat.getColor(ctx, messageColor));
		if (actionColor == null) {
			actionColor = ColorUtilities.getActiveColorId(nightMode);
		}
		tvAction.setTextColor(ContextCompat.getColor(ctx, actionColor));
		if (maxLines != null) {
			tvMessage.setMaxLines(maxLines);
		}
		if (backgroundColor == null) {
			backgroundColor = nightMode ? R.color.list_background_color_dark : R.color.color_black;
		}
		view.setBackgroundColor(ContextCompat.getColor(ctx, backgroundColor));
	}

	public static void setupSnackbarVerticalLayout(Snackbar snackbar) {
		View view = snackbar.getView();
		Context ctx = view.getContext();
		TextView messageView = view.findViewById(com.google.android.material.R.id.snackbar_text);
		TextView actionView = view.findViewById(com.google.android.material.R.id.snackbar_action);
		ViewParent parent = actionView.getParent();
		if (parent instanceof SnackbarContentLayout) {
			((SnackbarContentLayout) parent).removeView(actionView);
			((SnackbarContentLayout) parent).removeView(messageView);
			LinearLayout container = new LinearLayout(ctx);
			container.setOrientation(LinearLayout.VERTICAL);
			container.addView(messageView);
			container.addView(actionView);
			((SnackbarContentLayout) parent).addView(container);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			actionView.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
			container.setLayoutParams(params);
		}
		try {
			snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
		} catch (Throwable e) {
		}
	}

	public static void rotateImageByLayoutDirection(ImageView image) {
		if (image == null) {
			return;
		}
		int rotation = AndroidUtils.getLayoutDirection(image.getContext()) == ViewCompat.LAYOUT_DIRECTION_RTL ? 180 : 0;
		image.setRotationY(rotation);
	}

	public static void updateCustomRadioButtons(Context app, View buttonsView, boolean nightMode,
	                                            CustomRadioButtonType buttonType) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int textColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int radius = AndroidUtils.dpToPx(app, 4);
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);

		View startButtonContainer = buttonsView.findViewById(R.id.left_button_container);
		View centerButtonContainer = buttonsView.findViewById(R.id.center_button_container);
		View endButtonContainer = buttonsView.findViewById(R.id.right_button_container);

		GradientDrawable background = new GradientDrawable();
		background.setColor(ColorUtilities.getColorWithAlpha(activeColor, 0.1f));
		background.setStroke(AndroidUtils.dpToPx(app, 1.5f), ColorUtilities.getColorWithAlpha(activeColor, 0.5f));
		if (buttonType == CustomRadioButtonType.START) {
			if (isLayoutRtl) {
				background.setCornerRadii(new float[] {0, 0, radius, radius, radius, radius, 0, 0});
			} else {
				background.setCornerRadii(new float[] {radius, radius, 0, 0, 0, 0, radius, radius});
			}
			TextView startButtonText = startButtonContainer.findViewById(R.id.left_button);
			TextView endButtonText = endButtonContainer.findViewById(R.id.right_button);

			endButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			endButtonText.setTextColor(activeColor);
			startButtonContainer.setBackground(background);
			startButtonText.setTextColor(textColor);

			if (centerButtonContainer != null) {
				TextView centerButtonText = centerButtonContainer.findViewById(R.id.center_button);
				centerButtonText.setTextColor(activeColor);
				centerButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
		} else if (buttonType == CustomRadioButtonType.CENTER) {
			background.setCornerRadii(new float[] {0, 0, 0, 0, 0, 0, 0, 0});
			centerButtonContainer.setBackground(background);
			AndroidUiHelper.updateVisibility(centerButtonContainer, true);

			TextView centerButtonText = centerButtonContainer.findViewById(R.id.center_button);
			centerButtonText.setTextColor(textColor);

			if (endButtonContainer != null) {
				TextView endButtonText = endButtonContainer.findViewById(R.id.right_button);
				endButtonText.setTextColor(activeColor);
				endButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
			if (startButtonContainer != null) {
				TextView startButtonText = startButtonContainer.findViewById(R.id.left_button);
				startButtonText.setTextColor(activeColor);
				startButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
		} else {
			if (isLayoutRtl) {
				background.setCornerRadii(new float[] {radius, radius, 0, 0, 0, 0, radius, radius});
			} else {
				background.setCornerRadii(new float[] {0, 0, radius, radius, radius, radius, 0, 0});
			}
			TextView startButtonText = startButtonContainer.findViewById(R.id.left_button);
			TextView endButtonText = endButtonContainer.findViewById(R.id.right_button);

			endButtonContainer.setBackground(background);
			endButtonText.setTextColor(textColor);
			startButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			startButtonText.setTextColor(activeColor);

			if (centerButtonContainer != null) {
				TextView centerButtonText = centerButtonContainer.findViewById(R.id.center_button);
				centerButtonText.setTextColor(activeColor);
				centerButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
		}
	}

	public static void setupCompoundButtonDrawable(Context ctx, boolean nightMode, @ColorInt int activeColor, Drawable drawable) {
		int inactiveColor = ColorUtilities.getDefaultIconColor(ctx, nightMode);
		int[][] states = {
				new int[] {-android.R.attr.state_checked},
				new int[] {android.R.attr.state_checked}
		};
		ColorStateList csl = new ColorStateList(states, new int[] {inactiveColor, activeColor});
		DrawableCompat.setTintList(DrawableCompat.wrap(drawable), csl);
	}

	public static void setupCompoundButton(boolean nightMode, @ColorInt int activeColor, CompoundButton compoundButton) {
		if (compoundButton == null) {
			return;
		}
		Context ctx = compoundButton.getContext();
		int inactiveColorPrimary = ContextCompat.getColor(ctx, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		int inactiveColorSecondary = ColorUtilities.getColorWithAlpha(inactiveColorPrimary, 0.45f);
		setupCompoundButton(compoundButton, activeColor, inactiveColorPrimary, inactiveColorSecondary);
	}

	public static void setupCompoundButton(CompoundButton compoundButton, boolean nightMode, CompoundButtonType type) {
		if (compoundButton == null) {
			return;
		}
		OsmandApplication app = (OsmandApplication) compoundButton.getContext().getApplicationContext();
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		@ColorInt int inactiveColorPrimary = ContextCompat.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		@ColorInt int inactiveColorSecondary = ColorUtilities.getColorWithAlpha(inactiveColorPrimary, 0.45f);
		switch (type) {
			case PROFILE_DEPENDENT:
				ApplicationMode appMode = app.getSettings().getApplicationMode();
				activeColor = appMode.getProfileColor(nightMode);
				break;
			case TOOLBAR:
				activeColor = Color.WHITE;
				inactiveColorPrimary = activeColor;
				inactiveColorSecondary = ColorUtilities.getColorWithAlpha(Color.BLACK, 0.25f);
				break;
		}
		setupCompoundButton(compoundButton, activeColor, inactiveColorPrimary, inactiveColorSecondary);
	}

	public static Drawable getStrokedBackgroundForCompoundButton(@NonNull OsmandApplication app, int highlightColorDay, int highlightColorNight, boolean checked, boolean nightMode) {
		GradientDrawable background = (GradientDrawable) AppCompatResources.getDrawable(app,
				R.drawable.bg_select_group_button_outline);
		if (background != null) {
			int highlightColor = ContextCompat.getColor(app, nightMode ?
					highlightColorNight : highlightColorDay);
			int strokedColor = AndroidUtils.getColorFromAttr(getThemedContext(app, nightMode),
					R.attr.stroked_buttons_and_links_outline);
			background = (GradientDrawable) background.mutate();
			if (checked) {
				background.setStroke(0, Color.TRANSPARENT);
				background.setColor(highlightColor);
			} else {
				background.setStroke(app.getResources().getDimensionPixelSize(R.dimen.map_button_stroke), strokedColor);
			}
		}
		return background;
	}

	public static void setupCompoundButton(CompoundButton compoundButton,
										   @ColorInt int activeColor,
										   @ColorInt int inactiveColorPrimary,
										   @ColorInt int inactiveColorSecondary) {
		if (compoundButton == null) {
			return;
		}
		int[][] states = {
				new int[] {-android.R.attr.state_enabled},
				new int[] {-android.R.attr.state_checked},
				new int[] {android.R.attr.state_checked}
		};
		if (compoundButton instanceof SwitchCompat) {
			int[] thumbColors = {inactiveColorPrimary, inactiveColorPrimary, activeColor};
			int[] trackColors = {inactiveColorSecondary, inactiveColorSecondary, inactiveColorSecondary};

			SwitchCompat sc = (SwitchCompat) compoundButton;
			DrawableCompat.setTintList(DrawableCompat.wrap(sc.getThumbDrawable()), new ColorStateList(states, thumbColors));
			DrawableCompat.setTintList(DrawableCompat.wrap(sc.getTrackDrawable()), new ColorStateList(states, trackColors));
		} else if (compoundButton instanceof TintableCompoundButton) {
			int[] colors = {inactiveColorPrimary, inactiveColorPrimary, activeColor};
			ColorStateList csl = new ColorStateList(states, colors);
			((TintableCompoundButton) compoundButton).setSupportButtonTintList(csl);
		}
		compoundButton.setBackgroundColor(Color.TRANSPARENT);
	}

	public static void setupToolbarOverflowIcon(Toolbar toolbar, @DrawableRes int iconId, @ColorRes int colorId) {
		Context ctx = toolbar.getContext();
		if (ctx != null) {
			Drawable icon = ContextCompat.getDrawable(ctx, iconId);
			toolbar.setOverflowIcon(icon);
			if (icon != null) {
				int color = ContextCompat.getColor(ctx, colorId);
				DrawableCompat.setTint(icon.mutate(), color);
				toolbar.setOverflowIcon(icon);
			}
		}
	}

	public static ViewGroup createSliderView(@NonNull Context ctx, boolean nightMode) {
		return (ViewGroup) getInflater(ctx, nightMode).inflate(R.layout.slider, null, false);
	}

	public static void setupSlider(Slider slider, boolean nightMode, @ColorInt Integer activeColor) {
		setupSlider(slider, nightMode, activeColor, false);
	}

	public static void setupSlider(Slider slider, boolean nightMode,
	                               @ColorInt Integer activeColor, boolean showTicks) {
		Context ctx = slider.getContext();
		if (ctx == null) {
			return;
		}
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		ctx = new ContextThemeWrapper(ctx, themeId);

		// colors
		int[][] states = {
				new int[] {android.R.attr.state_enabled},
				new int[] {-android.R.attr.state_enabled}
		};
		if (activeColor == null) {
			activeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.active_color_basic);
		}
		int activeDisableColor = ColorUtilities.getColorWithAlpha(activeColor, 0.25f);
		ColorStateList activeCsl = new ColorStateList(states, new int[] {activeColor, activeDisableColor});
		int inactiveColor = ColorUtilities.getColorWithAlpha(activeColor, 0.5f);
		int inactiveDisableColor = ContextCompat.getColor(ctx, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		ColorStateList inactiveCsl = new ColorStateList(states, new int[] {inactiveColor, inactiveDisableColor});
		slider.setTrackActiveTintList(activeCsl);
		slider.setTrackInactiveTintList(inactiveCsl);
		slider.setHaloTintList(activeCsl);
		slider.setThumbTintList(activeCsl);
		int colorBlack = ContextCompat.getColor(ctx, R.color.color_black);
		int ticksColor = showTicks ?
				(nightMode ? colorBlack : ColorUtilities.getColorWithAlpha(colorBlack, 0.5f)) :
				Color.TRANSPARENT;
		slider.setTickTintList(new ColorStateList(states, new int[] {ticksColor, ticksColor}));

		// sizes
		slider.setThumbRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_size));
		slider.setHaloRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_halo_size));
		slider.setTrackHeight(ctx.getResources().getDimensionPixelSize(R.dimen.slider_track_height));

		// label behavior
		slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
	}

	public static void setupSlider(RangeSlider slider, boolean nightMode,
								   @ColorInt Integer activeColor, boolean showTicks) {
		Context ctx = slider.getContext();
		if (ctx == null) {
			return;
		}
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		ctx = new ContextThemeWrapper(ctx, themeId);

		// colors
		int[][] states = {
				new int[] {android.R.attr.state_enabled},
				new int[] {-android.R.attr.state_enabled}
		};
		if (activeColor == null) {
			activeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.active_color_basic);
		}
		int activeDisableColor = ColorUtilities.getColorWithAlpha(activeColor, 0.25f);
		ColorStateList activeCsl = new ColorStateList(states, new int[] {activeColor, activeDisableColor});
		int inactiveColor = ContextCompat.getColor(ctx, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		ColorStateList inactiveCsl = new ColorStateList(states, new int[] {activeDisableColor, inactiveColor});
		slider.setTrackActiveTintList(activeCsl);
		slider.setTrackInactiveTintList(inactiveCsl);
		slider.setHaloTintList(activeCsl);
		slider.setThumbTintList(activeCsl);
		int colorBlack = ContextCompat.getColor(ctx, R.color.color_black);
		int ticksColor = showTicks ?
				(nightMode ? colorBlack : ColorUtilities.getColorWithAlpha(colorBlack, 0.5f)) :
				Color.TRANSPARENT;
		slider.setTickTintList(new ColorStateList(states, new int[] {ticksColor, ticksColor}));

		// sizes
		slider.setThumbRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_size));
		slider.setHaloRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_halo_size));
		slider.setTrackHeight(ctx.getResources().getDimensionPixelSize(R.dimen.slider_track_height));

		// label behavior
		slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
	}

	public static void setupDialogButton(boolean nightMode, View buttonView, DialogButtonType buttonType, @StringRes int buttonTextId) {
		setupDialogButton(nightMode, buttonView, buttonType, buttonView.getContext().getString(buttonTextId));
	}

	public static void setupDialogButton(boolean nightMode, View buttonView, DialogButtonType buttonType, CharSequence buttonText) {
		setupDialogButton(nightMode, buttonView, buttonType, buttonText, INVALID_ID);
	}

	public static void setupDialogButton(boolean nightMode, View buttonView, DialogButtonType buttonType, CharSequence buttonText, int iconResId) {
		Context ctx = buttonView.getContext();
		TextViewEx buttonTextView = buttonView.findViewById(R.id.button_text);
		View buttonContainer = buttonView.findViewById(R.id.button_container);
		int textAndIconColorResId = INVALID_ID;
		switch (buttonType) {
			case PRIMARY:
				AndroidUtils.setBackground(ctx, buttonContainer, nightMode, R.drawable.ripple_solid_light, R.drawable.ripple_solid_dark);
				AndroidUtils.setBackground(ctx, buttonView, nightMode, R.drawable.dlg_btn_primary_light, R.drawable.dlg_btn_primary_dark);
				textAndIconColorResId = nightMode ? R.color.dlg_btn_primary_text_dark : R.color.dlg_btn_primary_text_light;
				break;
			case PRIMARY_HARMFUL:
				AndroidUtils.setBackground(ctx, buttonContainer, nightMode, R.drawable.ripple_solid_light, R.drawable.ripple_solid_dark);
				AndroidUtils.setBackground(buttonView, AppCompatResources.getDrawable(ctx, R.drawable.dlg_btn_primary_harmfull));
				textAndIconColorResId = nightMode ? R.color.dlg_btn_primary_text_dark : R.color.dlg_btn_primary_text_light;
				break;
			case SECONDARY:
				AndroidUtils.setBackground(ctx, buttonContainer, nightMode, R.drawable.ripple_solid_light, R.drawable.ripple_solid_dark);
				AndroidUtils.setBackground(ctx, buttonView, nightMode, R.drawable.dlg_btn_secondary_light, R.drawable.dlg_btn_secondary_dark);
				textAndIconColorResId = nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
				break;
			case SECONDARY_HARMFUL:
				AndroidUtils.setBackground(ctx, buttonContainer, nightMode, R.drawable.ripple_solid_light, R.drawable.ripple_solid_dark);
				AndroidUtils.setBackground(ctx, buttonView, nightMode, R.drawable.dlg_btn_secondary_light, R.drawable.dlg_btn_secondary_dark);
				textAndIconColorResId = R.color.color_osm_edit_delete;
				break;
			case SECONDARY_ACTIVE:
				AndroidUtils.setBackground(ctx, buttonContainer, nightMode, R.drawable.ripple_solid_light, R.drawable.ripple_solid_dark);
				AndroidUtils.setBackground(ctx, buttonView, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);
				textAndIconColorResId = nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
				break;
			case STROKED:
				AndroidUtils.setBackground(ctx, buttonContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
				AndroidUtils.setBackground(ctx, buttonView, nightMode, R.drawable.dlg_btn_stroked_light, R.drawable.dlg_btn_stroked_dark);
				textAndIconColorResId = nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
				break;
		}
		if (textAndIconColorResId != INVALID_ID) {
			ColorStateList colorStateList = ContextCompat.getColorStateList(ctx, textAndIconColorResId);
			buttonTextView.setText(buttonText);
			buttonTextView.setTextColor(colorStateList);
			buttonTextView.setEnabled(buttonView.isEnabled());
			if (iconResId != INVALID_ID) {
				Drawable icon = tintDrawable(AppCompatResources.getDrawable(ctx, iconResId), ContextCompat.getColor(ctx, textAndIconColorResId));
				buttonTextView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				buttonTextView.setCompoundDrawablePadding(AndroidUtils.dpToPx(ctx, ctx.getResources().getDimension(R.dimen.content_padding_half)));
			}
		}
	}

	public static LayoutInflater getInflater(Context ctx, boolean nightMode) {
		return LayoutInflater.from(getThemedContext(ctx, nightMode));
	}

	public static Context getThemedContext(Context context, boolean nightMode) {
		return getThemedContext(context, nightMode, R.style.OsmandLightTheme, R.style.OsmandDarkTheme);
	}

	public static Context getThemedContext(Context context, boolean nightMode, int lightStyle, int darkStyle) {
		return new ContextThemeWrapper(context, nightMode ? darkStyle : lightStyle);
	}

	public static void setMargins(View v, int s, int t, int e, int b) {
		if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			AndroidUtils.setMargins(p, s, t, e, b);
			v.requestLayout();
		}
	}

	public static SpannableString createSpannableString(@NonNull String text, int style, @NonNull String... textToStyle) {
		SpannableString spannable = new SpannableString(text);
		for (String t : textToStyle) {
			setSpan(spannable, new StyleSpan(style), text, t);
		}
		return spannable;
	}

	private static void setSpan(@NonNull SpannableString spannable,
	                            @NonNull Object styleSpan,
	                            @NonNull String text, @NonNull String t) {
		try {
			int startIndex = text.indexOf(t);
			spannable.setSpan(
					styleSpan,
					startIndex,
					startIndex + t.length(),
					Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		} catch (RuntimeException e) {
			LOG.error("Error trying to find index of " + t + " " + e);
		}
	}

	public static SpannableString createCustomFontSpannable(@NonNull Typeface typeface, @NonNull String text, @NonNull String... textToStyle) {
		SpannableString spannable = new SpannableString(text);
		for (String s : textToStyle) {
			setSpan(spannable, new CustomTypefaceSpan(typeface), text, s);
		}
		return spannable;
	}
}