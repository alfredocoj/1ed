package br.com.livrowebservices.carros.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import br.com.livrowebservices.carros.R;
import br.com.livrowebservices.carros.activity.CarroActivity;
import br.com.livrowebservices.carros.domain.Carro;
import br.com.livrowebservices.carros.domain.CarroService;
import br.com.livrowebservices.carros.fragment.adapter.CarroAdapter;
import br.com.livrowebservices.carros.utils.BroadcastUtil;
import livroandroid.lib.fragment.BaseFragment;

/**
 * Created by ricardo on 12/06/15.
 */
public class FavoritosFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private List<Carro> carros;
    private SwipeRefreshLayout swipeLayout;

    // Action Bar de Contexto
    private ActionMode actionMode;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            listaCarros(false);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, new IntentFilter(BroadcastUtil.ACTION_REFRESH_FAVORITOS));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favoritos, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Swipe to Refresh
        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeToRefresh);
        swipeLayout.setOnRefreshListener(OnRefreshListener());
        swipeLayout.setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_2,
                R.color.refresh_progress_3);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listaCarros(false);
    }

    // Task para buscar os carros favoritados
    private class GetCarrosTask extends BaseTask<List<Carro>> {

        @Override
        public List<Carro> execute() throws Exception {
            carros = CarroService.getCarrosFromDB(getContext());
            return carros;
        }

        @Override
        public void updateView(List<Carro> carros) {
            if (carros != null) {
                // O correto seria validar se excluiu e recarregar a lista.
//                listaCarros(true);

                // Atualiza a view na UI Thread
                recyclerView.setAdapter(new CarroAdapter(getContext(), carros, onClickCarro()));
            }
        }
    }

    protected CarroAdapter.CarroOnClickListener onClickCarro() {
        return new CarroAdapter.CarroOnClickListener() {
            @Override
            public void onClickCarro(CarroAdapter.CarrosViewHolder holder, int idx) {
                Carro c = carros.get(idx);

                if (actionMode == null) {
                    ImageView img = holder.img;

                    Intent intent = new Intent(getActivity(), CarroActivity.class);
                    intent.putExtra("carro", Parcels.wrap(c));
                    intent.putExtra("favorited",true);
                    String key = getString(R.string.transition_key);

                    // Compat
                    ActivityOptionsCompat opts = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), img, key);
                    ActivityCompat.startActivity(getActivity(), intent, opts.toBundle());
                } else {
                    // Seleciona o carro e atualiza a lista
                    c.selected = !c.selected;
                    updateActionModeTitle();
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onLongClickCarro(CarroAdapter.CarrosViewHolder holder, int idx) {
                if (actionMode != null) {
                    return;
                }

                //Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                actionMode = getAppCompatActivity().startSupportActionMode(getActionModeCallback());

                Carro c = carros.get(idx);
                c.selected = true;
                recyclerView.getAdapter().notifyDataSetChanged();

                updateActionModeTitle();
            }
        };
    }

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_frag_carros, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            toast("Faça a busca");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    private SwipeRefreshLayout.OnRefreshListener OnRefreshListener() {
        return new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                listaCarros(true);
            }
        };
    }

    private void listaCarros(boolean pullToRefresh) {
        startTask("carros", new GetCarrosTask(), pullToRefresh ? R.id.swipeToRefresh : R.id.progress);
    }

    private ActionMode.Callback getActionModeCallback() {
        return new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.menu_frag_carros_context, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                List<Carro> selectedCarros = getSelectedCarros();
                if (item.getItemId() == R.id.action_remove) {
                    deletarCarrosSelecionados();
                } else if (item.getItemId() == R.id.action_share) {
                    toast("Compartilhar: " + selectedCarros);
                }
                // Encerra o action mode
                mode.finish();
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Limpa o ActionMode e carros selecionados
                actionMode = null;
                for (Carro c : carros) {
                    c.selected = false;
                }
                // Atualiza a lista
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        };
    }

    // Deletar carros selecionados ao abrir a CAB
    private void deletarCarrosSelecionados() {
        final List<Carro> selectedCarros = getSelectedCarros();

        if(selectedCarros.size() > 0) {
            startTask("deletar",new BaseTask(){
                @Override
                public Object execute() throws Exception {
                    boolean ok = CarroService.deleteFromDb(getContext(), selectedCarros);
                    if(ok) {
                        // Se excluiu do banco, remove da lista da tela.
                        for (Carro c : selectedCarros) {
                            carros.remove(c);
                        }
                    }
                    return null;
                }

                @Override
                public void updateView(Object count) {
                    super.updateView(count);
                    // Mostra mensagem de sucesso
                    snack(recyclerView, selectedCarros.size() + " carros desfavoritados com sucesso");
                    // Atualiza a lista de carros
                    //listaCarros(true);
                    // Atualiza a lista
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            });
        }
    }

    private List<Carro> getSelectedCarros() {
        List<Carro> list = new ArrayList<Carro>();
        for (Carro c : carros) {
            if (c.selected) {
                list.add(c);
            }
        }
        return list;
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            actionMode.setTitle("Selecione os carros.");
            actionMode.setSubtitle(null);
            List<Carro> selectedCarros = getSelectedCarros();
            if (selectedCarros.size() == 0) {
                actionMode.finish();
            }else if (selectedCarros.size() == 1) {
                actionMode.setSubtitle("1 carro selecionado");
            } else if (selectedCarros.size() > 1) {
                actionMode.setSubtitle(selectedCarros.size() + " carros selecionados");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
    }
}