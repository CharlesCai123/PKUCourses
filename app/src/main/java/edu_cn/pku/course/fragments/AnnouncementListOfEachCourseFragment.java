package edu_cn.pku.course.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;

import java.util.ArrayList;

import edu_cn.pku.course.Utils;
import edu_cn.pku.course.activities.LoginActivity;
import edu_cn.pku.course.activities.R;
import edu_cn.pku.course.adapter.AnnouncementListRecyclerViewAdapter;
import edu_cn.pku.course.fragments.AnnouncementListFragment.AnnouncementInfo;


public class AnnouncementListOfEachCourseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private AnnouncementLoadingTask mLoadingTask = null;
    private RecyclerView mRecyclerView;
    private AnnouncementListRecyclerViewAdapter adapter;
    private SwipeRefreshLayout mAnnouncementListSwipeContainer;
    private static final String CourseId = "param1";

    private String courseId;

    public AnnouncementListOfEachCourseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     *
     * @return A new instance of fragment CoursesListFragment.
     */
    public static AnnouncementListOfEachCourseFragment newInstance(String courseId) {
        AnnouncementListOfEachCourseFragment fragment = new AnnouncementListOfEachCourseFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        args.putString(CourseId, courseId);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseId = getArguments().getString(CourseId);
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_announcement_list, container, false);
        // 查找xml文件中的对象并保存进Java变量
        mRecyclerView = linearLayout.findViewById(R.id.recycler_announcement_list);
        mAnnouncementListSwipeContainer = linearLayout.findViewById(R.id.announcement_swipe_container);

        // 设置动画
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down);
        mAnnouncementListSwipeContainer.setLayoutAnimation(animation);
        // 设置刷新的监听类为此类（监听函数onRefresh）
        mAnnouncementListSwipeContainer.setOnRefreshListener(this);

        FragmentActivity fa = getActivity();
        // 为了消除编译器Warning，需要判断一下是不是null，其实这基本上不可能出现null
        if (fa == null) {
            Snackbar.make(mRecyclerView, "null getActivity!", Snackbar.LENGTH_SHORT).show();
            return linearLayout;
        }

        // SharedPreferences sharedPreferences = fa.getSharedPreferences("pinnedAnnouncementList", Context.MODE_PRIVATE);
        adapter = new AnnouncementListRecyclerViewAdapter(new ArrayList<AnnouncementInfo>());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(adapter);

        // 显示Loading的小动画，并在后台读取课程列表
        showLoading(true);
        mLoadingTask = new AnnouncementLoadingTask();
        mLoadingTask.execute((Void) null);

        return linearLayout;
    }


    @Override
    public void onRefresh() {
        if (mLoadingTask == null) {
            mLoadingTask = new AnnouncementLoadingTask();
            mLoadingTask.execute((Void) null);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showLoading(final boolean show) {
        // 逐渐显示mRecyclerView的小动画
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        mRecyclerView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
        mAnnouncementListSwipeContainer.setRefreshing(show);
    }

    @SuppressLint("StaticFieldLeak")
    private class AnnouncementLoadingTask extends AsyncTask<Void, Void, String> {

        AnnouncementLoadingTask() {
        }

        //主要是有个course id一串数字是不一样的
        @Override
        protected String doInBackground(Void... params) {
            return Utils.courseHttpGetRequest("http://course.pku.edu.cn/webapps/blackboard/execute/announcement?method=search&course_id=" + courseId);
        }

        @Override
        protected void onPostExecute(final String str) {
            mLoadingTask = null;
            showLoading(false);

            // 出现了错误
            if (str.startsWith(Utils.errorPrefix)) {
                if (str.equals(Utils.errorPrefix + Utils.errorPasswordIncorrect)) {
                    // 密码错误
                    try {
                        signOut();
                    } catch (Exception e) {
                        Snackbar.make(mRecyclerView, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    // 其他网络错误
                    Snackbar.make(mRecyclerView, str, Snackbar.LENGTH_SHORT).show();
                }
            } else {
                // 解析返回的HTML
                String[] rawSplit = str.split("<h3 class");
                ArrayList<AnnouncementInfo> announcement_list = new ArrayList<>();

                FragmentActivity fa = getActivity();
                if (fa == null) {
                    Snackbar.make(mRecyclerView, "null getActivity!", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                // SharedPreferences sharedPreferences = fa.getSharedPreferences("pinnedAnnouncementList", Context.MODE_PRIVATE);
                // Set<String> hset = sharedPreferences.getStringSet("key", null);
                // if (hset == null)
                //     hset = new HashSet<>();
//这里是提取关键的原始字符串！！！不用分割？为什么wcb哪里把他分割了啊....还有我应该【0】元素是没有我要的东西的，从1开始？
                for (int i = 1; i < rawSplit.length; i++) {
                    String basicInfo = Utils.betweenStrings(rawSplit[i], "transparent", " <p><div class=");
                    String contents = Utils.betweenStrings(rawSplit[i], "<p><div class=\"vtbegenerated\">", "<div class=\"announcementInfo\">").split("</div></p>\n" +
                            "\t\t\t\t\t\t {4}</div>\n")[0];
                    String authorInfo = Utils.lastBetweenStrings(rawSplit[i], "<div class=\"announcementInfo\">", "</div>")
                            .replaceAll("<p>", "")
                            .replaceFirst("</p>", "<br>")
                            .replaceAll("</p>", "")
                            .replaceAll("\t", "");
                    AnnouncementInfo ai;
                    ai = new AnnouncementInfo(basicInfo, contents, authorInfo);
                    announcement_list.add(ai);
                }

                adapter.updateList(announcement_list);
                // 显示课程列表的fancy的动画
                mRecyclerView.scheduleLayoutAnimation();
            }
        }

        // 没什么用的函数
        @Override
        protected void onCancelled() {
            mLoadingTask = null;
            showLoading(false);
        }
    }

    public void signOut() throws Exception {
        FragmentActivity fa = getActivity();
        if (fa == null) {
            throw new Exception("Unknown Error: Null getActivity()!");
        }
        SharedPreferences sharedPreferences = fa.getSharedPreferences("login_info", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
