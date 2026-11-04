package com.dev.dynamicform.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dev.dynamicform.R
import com.dev.dynamicform.databinding.FragmentFormBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FormFragment : Fragment() {

    private var _binding: FragmentFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FormViewModel
    private lateinit var formAdapter: FormAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FormViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        formAdapter = FormAdapter(
            getValue = { key -> viewModel.fieldValues[key].orEmpty() },
            isRequired = { field -> viewModel.isFieldRequired(field) },
            onValueChanged = { key, value -> viewModel.onValueChanged(key, value) },
            onOptionSelected = { key, option -> viewModel.onOptionSelected(key, option) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = formAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            hideKeyboard()
            viewModel.onNextClicked()
        }
        binding.btnPrevious.setOnClickListener {
            hideKeyboard()
            viewModel.onPreviousClicked()
        }
        binding.btnRetry.setOnClickListener {
            viewModel.loadForm()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        view?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (loading) View.GONE else View.VISIBLE
            binding.bottomBar.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.errorLayout.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.tvError.text = error.orEmpty()
            binding.recyclerView.visibility = if (error != null) View.GONE else View.VISIBLE
            binding.bottomBar.visibility = if (error != null) View.GONE else View.VISIBLE
        }

        viewModel.currentPageIndex.observe(viewLifecycleOwner) { updatePage() }
        viewModel.pages.observe(viewLifecycleOwner) { updatePage() }

        viewModel.fieldErrors.observe(viewLifecycleOwner) { errors ->
            formAdapter.updateErrors(errors)
        }

        viewModel.submissionSuccess.observe(viewLifecycleOwner) { summary ->
            summary?.let { showSuccessDialog(it) }
        }
    }

    private fun updatePage() {
        val pages = viewModel.pages.value.orEmpty()
        val index = viewModel.currentPageIndex.value ?: 0
        if (pages.isEmpty()) return

        val total = pages.size
        val currentStep = index + 1

        binding.tvStep.text = getString(R.string.step_format, currentStep, total)
        binding.progressBar.max = total
        binding.progressBar.setProgressCompat(currentStep, true)

        binding.btnPrevious.isEnabled = index > 0
        binding.btnNext.text = if (index == total - 1) getString(R.string.submit) else getString(R.string.next)

        formAdapter.submitList(pages.getOrNull(index).orEmpty())
    }

    private fun showSuccessDialog(summary: Map<String, String>) {
        hideKeyboard()

        val summaryText = summary.entries.joinToString("\n\n") { (k, v) ->
            "$k:\n${v.ifBlank { "(Not provided)" }}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.submission_successful)
            .setMessage(summaryText)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                viewModel.resetForm()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
