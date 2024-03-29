package com.minageorge.navigationdemo.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import com.minageorge.navigationdemo.R
import com.minageorge.navigationdemo.store.ActivityBackPressed
import com.minageorge.navigationdemo.store.FragmentBackPressed
import com.minageorge.navigationdemo.store.OnNavigateToAnotherGraph
import com.minageorge.navigationdemo.store.rootDestinations
import com.minageorge.navigationdemo.utils.EventBus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseFragment : Fragment() {


    var toolbar: Toolbar? = null
    var navController: NavController? = null

    private val disposable = CompositeDisposable()
    private val appBarConfig = AppBarConfiguration.Builder(rootDestinations).build()

    @LayoutRes
    protected abstract fun getLayoutId(): Int

    protected abstract fun initFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addToDisposable(EventBus.listen(FragmentBackPressed::class.java).subscribe {
            it?.let {
                onActivityBackPressed()
            }
        })
        canProcessBackFromActivity(true)
        initFragment()
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    fun setupToolbar(@IdRes toolbarId: Int) {
        toolbar = requireActivity().findViewById(toolbarId)
    }

    fun setupNavigation(@IdRes navHostId: Int) {
        toolbar?.let { toolbar ->
            navController = requireActivity().findNavController(navHostId).also { controller ->
                NavigationUI.setupWithNavController(toolbar, controller, appBarConfig)
            }
        } ?: kotlin.run {
            navController = requireActivity().findNavController(navHostId)
        }
    }

    fun onBackPressed(): Boolean {
        return navController?.navigateUp(appBarConfig) ?: false
    }

    fun popToRoot(inclusive: Boolean = false) {
        navController?.popBackStack(navController?.graph!!.startDestination, inclusive)
    }

    fun canProcessBackFromActivity(canBack: Boolean) {
        EventBus.publish(ActivityBackPressed(canBack))
    }

    fun navigateInside(@IdRes destId: Int, args: Bundle? = null) {
        view?.findNavController()?.navigate(destId, args)
    }

    fun navigateToActivity(intent: Intent, args: Bundle? = null, popCurrent: Boolean = false) {
        val activityNavigator = ActivityNavigator(context!!)
        activityNavigator.navigate(
            activityNavigator.createDestination().setIntent(intent),
            args,
            null,
            null
        )

        if (popCurrent) activityNavigator.popBackStack()
    }

    fun navigateToActivity(@IdRes destId: Int, args: Bundle? = null, popCurrent: Boolean = false) {
        view?.findNavController()?.navigate(destId, args)
        val activityNavigator = ActivityNavigator(context!!)
        if (popCurrent) activityNavigator.popBackStack()
    }


    fun navigateOutside(@IdRes navId: Int, @IdRes destId: Int, args: Bundle? = null) {
        Navigation.findNavController(requireActivity(), navId).navigate(destId, args)
        when (navId) {
            R.id.nav_host_one -> EventBus.publish(OnNavigateToAnotherGraph(0))
            R.id.nav_host_two -> EventBus.publish(OnNavigateToAnotherGraph(1))
            R.id.nav_host_three -> EventBus.publish(OnNavigateToAnotherGraph(2))
        }
        navController?.popBackStack()
    }

    open fun onActivityBackPressed() {

    }

    fun handleDeepLink(intent: Intent) = navController?.handleDeepLink(intent) ?: false

    fun addToDisposable(disposable: Disposable) {
        this.disposable.remove(disposable)
        this.disposable.add(disposable)
    }
}