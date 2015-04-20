package com.example.waterfall;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.example.waterfall.LazyScrollView.OnScrollListener;
import com.example.waterfall.widget.FlowView;


import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

	private LazyScrollView waterfall_scroll;
	private LinearLayout waterfall_container;
	private ArrayList<LinearLayout> waterfall_items;
	private Display display;
	private AssetManager asset_manager;
	private List<String> image_filenames;
	private final String image_path = "images";
	private Handler handler;
	private int item_width;

	private int column_count = Constants.COLUMN_COUNT;// 鏄剧ず鍒楁暟
	private int page_count = Constants.PICTURE_COUNT_PER_LOAD;// 姣忔鍔犺浇30寮犲浘鐗?

	private int current_page = 0;// 褰撳墠椤垫暟

	private int[] topIndex;
	private int[] bottomIndex;
	private int[] lineIndex;
	private int[] column_height;// 姣忓垪鐨勯珮搴?

	private HashMap<Integer, String> pins;

	private int loaded_count = 0;// 宸插姞杞芥暟閲?

	private HashMap<Integer, Integer>[] pin_mark = null;

	private Context context;

	private HashMap<Integer, FlowView> iviews;
	int scroll_height;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		display = this.getWindowManager().getDefaultDisplay();
		item_width = display.getWidth() / column_count;// 鏍规嵁灞忓箷澶у皬璁＄畻姣忓垪澶у皬
		asset_manager = getAssets();

		column_height = new int[column_count];
		context = this;
		iviews = new HashMap<Integer, FlowView>();
		pins = new HashMap<Integer, String>();
		pin_mark = new HashMap[column_count];

		this.lineIndex = new int[column_count];
		this.bottomIndex = new int[column_count];
		this.topIndex = new int[column_count];

		for (int i = 0; i < column_count; i++) {
			lineIndex[i] = -1;
			bottomIndex[i] = -1;
			pin_mark[i] = new HashMap();
		}

		InitLayout();

	}

	private void InitLayout() {
		waterfall_scroll = (LazyScrollView) findViewById(R.id.waterfall_scroll);

		waterfall_scroll.getView();
		waterfall_scroll.setOnScrollListener(new OnScrollListener() {

			public void onTop() {
				// 婊氬姩鍒版渶椤剁
				Log.d("LazyScroll", "Scroll to top");
			}

			public void onScroll() {

			}

			public void onBottom() {
				// 婊氬姩鍒版渶浣庣
				AddItemToContainer(++current_page, page_count);
			}

			public void onAutoScroll(int l, int t, int oldl, int oldt) {

				// Log.d("MainActivity",
				// String.format("%d  %d  %d  %d", l, t, oldl, oldt));

				// Log.d("MainActivity", "range:" + range);
				// Log.d("MainActivity", "range-t:" + (range - t));
				scroll_height = waterfall_scroll.getMeasuredHeight();
				Log.d("MainActivity", "scroll_height:" + scroll_height);

				if (t > oldt) {// 鍚戜笅婊氬姩
					if (t > 2 * scroll_height) {// 瓒呰繃涓ゅ睆骞曞悗

						for (int k = 0; k < column_count; k++) {

							LinearLayout localLinearLayout = waterfall_items
									.get(k);

							if (pin_mark[k].get(Math.min(bottomIndex[k] + 1,
									lineIndex[k])) <= t + 3 * scroll_height) {// 鏈?搴曢儴鐨勫浘鐗囦綅缃皬浜庡綋鍓峵+3*灞忓箷楂樺害

								((FlowView) waterfall_items.get(k).getChildAt(
										Math.min(1 + bottomIndex[k],
												lineIndex[k]))).Reload();

								bottomIndex[k] = Math.min(1 + bottomIndex[k],
										lineIndex[k]);

							}
							Log.d("MainActivity",
									"headIndex:" + topIndex[k] + "  footIndex:"
											+ bottomIndex[k] + "  headHeight:"
											+ pin_mark[k].get(topIndex[k]));
							if (pin_mark[k].get(topIndex[k]) < t - 2
									* scroll_height) {// 鏈洖鏀跺浘鐗囩殑鏈?楂樹綅缃?<t-涓ゅ?嶅睆骞曢珮搴?

								int i1 = topIndex[k];
								topIndex[k]++;
								((FlowView) localLinearLayout.getChildAt(i1))
										.recycle();
								Log.d("MainActivity", "recycle,k:" + k
										+ " headindex:" + topIndex[k]);

							}
						}

					}
				} else {// 鍚戜笂婊氬姩
					if (t > 2 * scroll_height) {// 瓒呰繃涓ゅ睆骞曞悗
						for (int k = 0; k < column_count; k++) {
							LinearLayout localLinearLayout = waterfall_items
									.get(k);
							if (pin_mark[k].get(bottomIndex[k]) > t + 3
									* scroll_height) {
								((FlowView) localLinearLayout
										.getChildAt(bottomIndex[k])).recycle();

								bottomIndex[k]--;
							}

							if (pin_mark[k].get(Math.max(topIndex[k] - 1, 0)) >= t
									- 2 * scroll_height) {
								((FlowView) localLinearLayout.getChildAt(Math
										.max(-1 + topIndex[k], 0))).Reload();
								topIndex[k] = Math.max(topIndex[k] - 1, 0);
							}
						}
					}

				}

			}
		});

		waterfall_container = (LinearLayout) this
				.findViewById(R.id.waterfall_container);
		handler = new Handler() {

			@Override
			public void dispatchMessage(Message msg) {

				super.dispatchMessage(msg);
			}

			@Override
			public void handleMessage(Message msg) {

				// super.handleMessage(msg);

				switch (msg.what) {
				case Constants.HANDLER_WHAT:

					FlowView v = (FlowView) msg.obj;
					int w = msg.arg1;
					int h = msg.arg2;
					// Log.d("MainActivity",
					// String.format(
					// "鑾峰彇瀹為檯View楂樺害:%d,ID锛?%d,columnIndex:%d,rowIndex:%d,filename:%s",
					// v.getHeight(), v.getId(), v
					// .getColumnIndex(), v.getRowIndex(),
					// v.getFlowTag().getFileName()));
					String f = v.getFileName();

					// 姝ゅ璁＄畻鍒楀??
					int columnIndex = GetMinValue(column_height);

					v.setColumnIndex(columnIndex);

					column_height[columnIndex] += h;

					pins.put(v.getId(), f);
					iviews.put(v.getId(), v);
					waterfall_items.get(columnIndex).addView(v);

					lineIndex[columnIndex]++;

					pin_mark[columnIndex].put(lineIndex[columnIndex],
							column_height[columnIndex]);
					bottomIndex[columnIndex] = lineIndex[columnIndex];
					break;
				}

			}

			@Override
			public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
				return super.sendMessageAtTime(msg, uptimeMillis);
			}
		};

		waterfall_items = new ArrayList<LinearLayout>();

		for (int i = 0; i < column_count; i++) {
			LinearLayout itemLayout = new LinearLayout(this);
			LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
					item_width, LayoutParams.WRAP_CONTENT);

			itemLayout.setPadding(2, 2, 2, 2);
			itemLayout.setOrientation(LinearLayout.VERTICAL);

			itemLayout.setLayoutParams(itemParam);
			waterfall_items.add(itemLayout);
			waterfall_container.addView(itemLayout);
		}

		// 鍔犺浇鎵?鏈夊浘鐗囪矾寰?
		try {
			image_filenames = Arrays.asList(asset_manager.list(image_path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// 绗竴娆″姞杞?
		AddItemToContainer(current_page, page_count);
	}

	private void AddItemToContainer(int pageindex, int pagecount) {
		
		int currentIndex = pageindex * pagecount;
		int imagecount = Constants.PICTURE_TOTAL_COUNT;// image_filenames.size();
		
		for (int i = currentIndex; i < pagecount * (pageindex + 1)
				&& i < imagecount; i++) {
			loaded_count++;
			Random rand = new Random();
			int r = rand.nextInt(image_filenames.size());
			AddImage(image_filenames.get(r),
					(int) Math.ceil(loaded_count / (double) column_count),
					loaded_count);
		}

	}

	private void AddImage(String filename, int rowIndex, int id) {

		FlowView item = new FlowView(context);
		// item.setColumnIndex(columnIndex);

		item.setRowIndex(rowIndex);
		item.setId(id);
		item.setViewHandler(handler);
		item.setFileName(image_path + "/" + filename);
		item.setItemWidth(item_width);
		item.LoadImage();
		// waterfall_items.get(columnIndex).addView(item);

	}

	private int GetMinValue(int[] array) {
		int m = 0;
		int length = array.length;
		for (int i = 0; i < length; ++i) {

			if (array[i] < array[m]) {
				m = i;
			}
		}
		return m;
	}
}
