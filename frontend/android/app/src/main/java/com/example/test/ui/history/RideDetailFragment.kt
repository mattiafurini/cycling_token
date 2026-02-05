package com.example.test.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.test.data.RideRepository
import com.example.test.databinding.FragmentRideDetailBinding
import java.util.Locale

class RideDetailFragment : Fragment() {

    private var _binding: FragmentRideDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRideDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val rideId = arguments?.getLong("ride_id") ?: return
        val ride = RideRepository.getRide(requireContext(), rideId) ?: return
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.textDetailDate.text = ride.getFormattedDate()
        binding.textDetailDistance.text = String.format(Locale.getDefault(), "%.2f km", ride.distanceKm)
        
        val minutes = ride.durationSeconds / 60
        val seconds = ride.durationSeconds % 60
        binding.textDetailTime.text = String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
        
        binding.textDetailTokens.text = "+${ride.tokensEarned}"
        
        binding.mapView.setPath(ride.path)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}