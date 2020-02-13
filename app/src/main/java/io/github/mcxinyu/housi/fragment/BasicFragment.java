package io.github.mcxinyu.housi.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.percent.PercentRelativeLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import eu.chainfire.libsuperuser.Shell;
import io.github.mcxinyu.housi.R;
import io.github.mcxinyu.housi.api.ApiRetrofit;
import io.github.mcxinyu.housi.api.SourceApiHelper;
import io.github.mcxinyu.housi.util.LogUtils;
import io.github.mcxinyu.housi.util.StateUtils;
import io.github.mcxinyu.housi.util.StaticValues;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by huangyuefeng on 2017/9/14.
 * Contact me : mcxinyu@gmail.com
 */
public class BasicFragment extends ABaseFragment {
    private static final String TAG = "BasicFragment";

    @BindView(R.id.this_container)
    PercentRelativeLayout mThisContainer;
    @BindView(R.id.icon_image_view)
    ImageView mIconImageView;
    @BindView(R.id.app_name)
    TextView mAppName;
    @BindView(R.id.app_description)
    TextView mAppDescription;
    @BindView(R.id.update_hosts_button)
    Button mUpdateHostsButton;
    @BindView(R.id.reset_hosts_button)
    Button mResetHostsButton;
    @BindView(R.id.read_hosts_button)
    Button mReadHostsButton;
    @BindView(R.id.button_activate)
    LinearLayout mButtonActivate;
    Unbinder unbinder;

    public static BasicFragment newInstance() {

        Bundle args = new Bundle();

        BasicFragment fragment = new BasicFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Callbacks mCallbacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callbacks) {
            mCallbacks = (Callbacks) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement BackHandledInterface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_basic, container, false);
        unbinder = ButterKnife.bind(this, view);

        mUpdateHostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHosts();
            }
        });

        mResetHostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetHosts();
            }
        });

        mReadHostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readHosts();
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private String getEmptyHostPath() {
        return getContext().getFilesDir().getAbsolutePath() + File.separator + StaticValues.EMPTY_HOST_NAME;
    }

    @WorkerThread
    private void initEmptyHostFile() {
        File emptyHostFile = new File(getEmptyHostPath());
        if (emptyHostFile.exists()) {
            return;
        }

        File file = new File(getEmptyHostPath());
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(StaticValues.EMPTY_HOST_VALUE);
            fileWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected String getToolBarTitle() {
        return getString(R.string.action_basic);
    }

    @Override
    protected int getMenuItemId() {
        return R.id.nav_basic;
    }

    private void readHosts() {
        // TODO: 2017/9/17
    }

    @SuppressWarnings("deprecation")
    private void updateHosts() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.do_on_update_hosts));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();

        SourceApiHelper.getSourceHosts(getContext(), ApiRetrofit.BASE_TEMP_URL)
                .map(new Func1<File, Integer>() {
                    @Override
                    public Integer call(File file) {
                        boolean suAvailable = Shell.SU.available();
                        if (!suAvailable) {
                            return -1;
                        }

                        List<String> suResult = Shell.SU.run(new String[]{
                                StaticValues.MOUNT_SYSTEM_RW,
                                "cp " + file.getAbsolutePath() + " " + StaticValues.SYSTEM_HOST_FILE_PATH,
                                StaticValues.MOUNT_SYSTEM_RO
                        });

                        if (suResult == null) {
                            return -2;
                        } else {
                            for (int i = 0; i < suResult.size(); i++) {
                                LogUtils.d(TAG, "suResult line " + i + " : " + suResult.get(i));
                            }
                            return suResult.size();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        dialog.dismiss();
                        Toast.makeText(getContext(), StateUtils.handleException(e), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(Integer integer) {
                        switch (integer) {
                            case -2:
                            case -1:
                                Toast.makeText(getContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                                mCallbacks.hasRoot(false);
                                break;
                            case 0:
                                Toast.makeText(getContext(), getString(R.string.update_hosts_success), Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(getContext(), getString(R.string.update_hosts_failure), Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    private void resetHosts() {
        new ResetHosts().execute();
    }

    @SuppressWarnings("deprecation")
    class ResetHosts extends AsyncTask<Void, Void, Void> {
        ProgressDialog dialog = null;
        boolean suAvailable = false;
        List<String> suResult = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getString(R.string.do_on_reset_hosts));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            suAvailable = Shell.SU.available();
            if (!suAvailable) {
                return null;
            }

            initEmptyHostFile();

            suResult = Shell.SU.run(new String[]{
                    StaticValues.MOUNT_SYSTEM_RW,
                    "cp " + getEmptyHostPath() + " " + StaticValues.SYSTEM_HOST_FILE_PATH,
                    StaticValues.MOUNT_SYSTEM_RO
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();

            if (!suAvailable) {
                Toast.makeText(getContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                mCallbacks.hasRoot(suAvailable);
            } else if (suResult != null) {
                for (int i = 0; i < suResult.size(); i++) {
                    Log.d(TAG, "suResult line " + i + " : " + suResult.get(i));
                }
                Toast.makeText(getContext(),
                        suResult.size() == 0 ? getString(R.string.reset_hosts_success) : getString(R.string.reset_hosts_failure),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public interface Callbacks {
        void hasRoot(boolean hasRoot);
    }
}
