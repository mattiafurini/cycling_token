package com.example.test.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.R
import com.example.test.data.RideRepository
import com.example.test.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    private fun updateList() {
        val rides = RideRepository.getRides(requireContext())
        if (rides.isEmpty()) {
            binding.textEmptyHistory.visibility = View.VISIBLE
            binding.recyclerHistory.visibility = View.GONE
        } else {
            binding.textEmptyHistory.visibility = View.GONE
            binding.recyclerHistory.visibility = View.VISIBLE
            binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
            binding.recyclerHistory.adapter = HistoryAdapter(rides) { ride ->
                val bundle = Bundle()
                bundle.putLong("ride_id", ride.id)
                findNavController().navigate(R.id.action_history_to_detail, bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}