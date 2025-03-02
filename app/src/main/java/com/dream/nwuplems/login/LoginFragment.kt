package com.dream.nwuplems.login

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.transition.TransitionInflater
import com.dream.nwuplems.R
import com.dream.nwuplems.database.AppDataBase
import com.dream.nwuplems.databinding.FragmentLoginBinding
import com.ethanhua.skeleton.Skeleton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by activityViewModels()
    private lateinit var binding: FragmentLoginBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.loginViewModel = viewModel
        binding.lifecycleOwner = this
        viewModel.snackMessage.observe(viewLifecycleOwner) {
            Snackbar.make(requireActivity().findViewById(R.id.rootView), it, Snackbar.LENGTH_LONG)
                .show()
        }
        viewModel.loginRetrys.observe(viewLifecycleOwner) {
            if (it >= 2) {
                binding.inputText3.visibility = View.VISIBLE
                binding.captchaImage.visibility = View.VISIBLE
            } else {
                binding.captchaImage.visibility = View.GONE
                binding.captchaImage.visibility = View.GONE
            }
        }
        viewModel.emsCookies.observe(viewLifecycleOwner) {
            //登录成功
            if (!checkAccount()) {
                //两次账号不一致则清除数据表
                Log.i("已清除数据库", "")
                GlobalScope.launch {
                    AppDataBase.getDatabase(requireContext()).clearAllTables()
                }
            }
            saveAccount()
            requireActivity().findViewById<TextView>(R.id.studentDrawerName).text =
                LoginUtilsNew.studentName
            requireActivity().findViewById<TextView>(R.id.studentDrawerID).text =
                LoginUtilsNew.studentID
            val action = LoginFragmentDirections.actionLoginFragmentToCourseTableFragment()

            val transInflater = TransitionInflater.from(requireContext())
            exitTransition = transInflater.inflateTransition(R.transition.explode)
            enterTransition = transInflater.inflateTransition(R.transition.fade)
            binding.root.findNavController().navigate(action)
        }
        //检查是否是第一次进入
        getAccount()
        if (isFirstEnterApp()) {
            userAgreement()
        }
        binding.privacyText.setOnClickListener { userAgreement() }
        val captchaSkeleton = Skeleton.bind(binding.captchaImage)
            .load(R.layout.layout_img_skeleton)
            .show()
        viewModel.captchaInProgress.observe(viewLifecycleOwner) {
            if (it) {
                captchaSkeleton.show()
            } else {
                captchaSkeleton.hide()
            }
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.button2.setOnClickListener {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(
                requireActivity().currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
            viewModel.login()
        }
    }

    private fun privacyPolicy() {
        val text = resources.getText(R.string.privacyPolicy)
        val alterDiaglog: AlertDialog.Builder =
            AlertDialog.Builder(requireContext())
        alterDiaglog.setTitle("隐私协议1.0") //文字
        alterDiaglog.setMessage(text) //提示消息
        alterDiaglog.setPositiveButton(
            "同意并继续"
        ) { _: DialogInterface, _: Int ->
            saveFirstEnterApp()
        }
        alterDiaglog.setNeutralButton(
            "退出应用"
        ) { _: DialogInterface, _: Int ->
            requireActivity().finish()
        }
        alterDiaglog.show()
    }

    private fun userAgreement() {
        val text = resources.getText(R.string.userAgreement)
        val alterDiaglog: AlertDialog.Builder =
            AlertDialog.Builder(requireContext())
        alterDiaglog.setTitle("用户协议1.0") //文字
        alterDiaglog.setMessage(text) //提示消息
        alterDiaglog.setPositiveButton(
            "同意并继续"
        ) { _: DialogInterface, _: Int ->
            privacyPolicy()
        }
        alterDiaglog.setNeutralButton(
            "退出应用"
        ) { _: DialogInterface, _: Int ->
            requireActivity().finish()
        }
        alterDiaglog.show()
    }

    private fun isFirstEnterApp(): Boolean {
        val sharedPreferences =
            requireActivity().getSharedPreferences("data", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isFirstEnterApp", true)
    }

    private fun saveFirstEnterApp() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstEnterApp", false)
        editor.apply()
    }

    private fun saveAccount() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("accountNumber", viewModel.accountText.value)
        editor.putString("passwordText", viewModel.passwordText.value)
        editor.apply()
    }

    private fun checkAccount(): Boolean {
        //检查是否和上次登录的账号一样
        val sharedPreferences =
            requireActivity().getSharedPreferences("data", Context.MODE_PRIVATE)
        val lastLogAccount = sharedPreferences.getString("accountNumber", "")
        if (lastLogAccount != null) {
            Log.i("上次登录", lastLogAccount)
        }
        viewModel.accountText.value?.let { Log.i("本次登录", it) }
        return lastLogAccount == viewModel.accountText.value
    }

    private fun getAccount() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("data", Context.MODE_PRIVATE)
        viewModel.accountText.value = sharedPreferences.getString("accountNumber", "")
        viewModel.passwordText.value = sharedPreferences.getString("passwordText", "")
    }
}
