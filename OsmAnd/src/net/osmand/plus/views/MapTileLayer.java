package net.osmand.plus.views;

import net.osmand.access.AccessibleToast;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.resources.ResourceManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;
import android.widget.Toast;
import net.osmand.util.MapUtils;

public class MapTileLayer extends BaseMapLayer {

	
	protected static final int emptyTileDivisor = 16;
	public static final int OVERZOOM_IN = 2;
	
	private final boolean mainMap;
	protected ITileSource map = null;
	protected MapTileAdapter mapTileAdapter = null;
	
	Paint paintBitmap;
	protected RectF bitmapToDraw = new RectF();
	protected Rect bitmapToZoom = new Rect();
	

	protected OsmandMapTileView view;
	protected ResourceManager resourceManager;
	private OsmandSettings settings;
	private boolean visible = true;

	
	public MapTileLayer(boolean mainMap){
		this.mainMap = mainMap;
	}
	
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		settings = view.getSettings();
		resourceManager = view.getApplication().getResourceManager();

		paintBitmap = new Paint();
		paintBitmap.setFilterBitmap(true);
		paintBitmap.setAlpha(getAlpha());
		
		if(mapTileAdapter != null && view != null){
			mapTileAdapter.initLayerAdapter(this, view);
		}
	}
	
	@Override
	public void setAlpha(int alpha) {
		super.setAlpha(alpha);
		if (paintBitmap != null) {
			paintBitmap.setAlpha(alpha);
		}
	}
	
	public void setMapTileAdapter(MapTileAdapter mapTileAdapter) {
		if(this.mapTileAdapter == mapTileAdapter){
			return;
		}
		if(this.mapTileAdapter != null){
			this.mapTileAdapter.onClear();
		}
		this.mapTileAdapter = mapTileAdapter;
		if(mapTileAdapter != null && view != null){
			mapTileAdapter.initLayerAdapter(this, view);
			mapTileAdapter.onInit();
		}
	}
	
	public void setMapForMapTileAdapter(ITileSource map, MapTileAdapter mapTileAdapter) {
		if(mapTileAdapter == this.mapTileAdapter){
			this.map = map;
		}
	}
	
	public void setMap(ITileSource map) {
		MapTileAdapter target = null;
		if(map instanceof TileSourceTemplate){
			if(TileSourceManager.RULE_YANDEX_TRAFFIC.equals(((TileSourceTemplate) map).getRule())){
				map = null;
				target = new YandexTrafficAdapter();
			}
			
		}
		this.map = map;
		setMapTileAdapter(target);
	}
	
	public MapTileAdapter getMapTileAdapter() {
		return mapTileAdapter;
	}
	

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		if ((map == null && mapTileAdapter == null) || !visible) {
			return;
		}
		if(mapTileAdapter != null){
			mapTileAdapter.onDraw(canvas, tileBox, drawSettings);
		}
		drawTileMap(canvas, tileBox);
	}

	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox) {
		if(map == null){
			return;
		}
		ResourceManager mgr = resourceManager;
		int nzoom = tileBox.getZoom();
		final QuadRect tilesRect = tileBox.getTileBounds();

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection  = 0;
		if (map.isEllipticYTile()) {
			ellipticTileCorrection = (float) (MapUtils.getTileEllipsoidNumberY(nzoom, tileBox.getLatitude()) - tileBox.getCenterTileY());
		}


		int left = (int) FloatMath.floor(tilesRect.left);
		int top = (int) FloatMath.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) FloatMath.ceil(tilesRect.right - left);
		int height = (int) FloatMath.ceil(tilesRect.bottom + ellipticTileCorrection - top);

		boolean useInternet = settings.USE_INTERNET_TO_DOWNLOAD_TILES.get()
					&& settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();
		int maxLevel = Math.min(view.getSettings().MAX_LEVEL_TO_DOWNLOAD_TILE.get(), map.getMaximumZoomSupported());
		int tileSize = map.getTileSize();
		boolean oneTileShown = false;

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int leftPlusI = left + i;
				int topPlusJ = top + j;

				int x1 = tileBox.getPixXFromTileXNoRot(leftPlusI);
				int x2 = tileBox.getPixXFromTileXNoRot(leftPlusI + 1);

				int y1 = tileBox.getPixYFromTileYNoRot(topPlusJ -  ellipticTileCorrection);
				int y2 = tileBox.getPixYFromTileYNoRot(topPlusJ + 1 -  ellipticTileCorrection);
				final int tileX = leftPlusI;
				final int tileY = topPlusJ;
				String ordImgTile = mgr.calculateTileId(map, tileX, tileY, nzoom);
				// asking tile image async
				boolean imgExist = mgr.tileExistOnFileSystem(ordImgTile, map, tileX, tileY, nzoom, false);
				Bitmap bmp = null;
				boolean originalBeLoaded = useInternet && nzoom <= maxLevel;
				if (imgExist || originalBeLoaded) {
					bmp = mgr.getTileImageForMapAsync(ordImgTile, map, tileX, tileY, nzoom, useInternet);
				}
				if (bmp == null) {
					int div = 2;
					// asking if there is small version of the map (in cache)
					String imgTile2 = mgr.calculateTileId(map, tileX / 2, tileY / 2, nzoom - 1);
					String imgTile4 = mgr.calculateTileId(map, tileX / 4, tileY / 4, nzoom - 2);
					if (originalBeLoaded || imgExist) {
						bmp = mgr.getTileImageFromCache(imgTile2);
						div = 2;
						if (bmp == null) {
							bmp = mgr.getTileImageFromCache(imgTile4);
							div = 4;
						}
					}
					if (!originalBeLoaded && !imgExist) {
						if (mgr.tileExistOnFileSystem(imgTile2, map, tileX / 2, tileY / 2, nzoom - 1, false)
								|| (useInternet && nzoom - 1 <= maxLevel)) {
							bmp = mgr.getTileImageForMapAsync(imgTile2, map, tileX / 2, tileY / 2, nzoom - 1, useInternet);
							div = 2;
						} else if (mgr.tileExistOnFileSystem(imgTile4, map, tileX / 4, tileY / 4, nzoom - 2, false)
								|| (useInternet && nzoom - 2 <= maxLevel)) {
							bmp = mgr.getTileImageForMapAsync(imgTile4, map, tileX / 4, tileY / 4, nzoom - 2, useInternet);
							div = 4;
						}
					}

					if (bmp != null) {
						int xZoom = ((left + i) % div) * tileSize / div;
						int yZoom = ((top + j) % div) * tileSize / div;
						bitmapToZoom.set(xZoom, yZoom, xZoom + tileSize / div, yZoom + tileSize / div);
						bitmapToDraw.set(x1, y1, x2 , y2);
						canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
						oneTileShown = true;
					}
				} else {
					bitmapToZoom.set(0, 0, tileSize, tileSize);
					bitmapToDraw.set(x1, y1, x2, y2 );
					canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
					oneTileShown = true;
				}
			}
		}
		
		if(mainMap && !oneTileShown && !useInternet && warningToSwitchMapShown < 3){
			if(resourceManager.getRenderer().containsLatLonMapData(view.getLatitude(), view.getLongitude(), nzoom)){
				AccessibleToast.makeText(view.getContext(), R.string.switch_to_vector_map_to_see, Toast.LENGTH_LONG).show();
				warningToSwitchMapShown++;
			}
		}
	}
	
	public int getSourceTileSize() {
		return map == null ? 256 : map.getTileSize();
	}
	
	
	@Override
	public int getMaximumShownMapZoom(){
		if(map == null){
			return 20;
		} else {
			return map.getMaximumZoomSupported() + OVERZOOM_IN;
		}
	}
	
	@Override
	public int getMinimumShownMapZoom(){
		if(map == null){
			return 1;
		} else {
			return map.getMinimumZoomSupported();
		}
	}
		
	@Override
	public void destroyLayer() {
		// TODO clear map cache
		setMapTileAdapter(null);
	}

	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		// TODO clear map cache
	}
	
	public ITileSource getMap() {
		return map;
	}
	

}
