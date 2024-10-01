package com.vote.VotingSystem.Service;

import com.vote.VotingSystem.Model.Candidate;
import com.vote.VotingSystem.Model.Gender;
import com.vote.VotingSystem.Repository.CandidateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CandidateServiceImpl implements CandidateService{

    private static long totalGain = 0;
    private static final int GAIN_PER_VOTE = 100; // 100 FCFA par vote

    @Autowired
    private CandidateRepository candidateRepository;


    @Override
    public Candidate saveCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    @Override
    public void deleteCandidate(Long id) {
        candidateRepository.deleteById(id);
    }

    @Override
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    @Override
    @Transactional
    public int incrementVoteCount(Long id) {
        Candidate candidate = getCandidateById(id);
        if (candidate != null)  {
            candidate.setVoteCount(candidate.getVoteCount() + 1);
            candidateRepository.save(candidate);
            incrementTotalGain(GAIN_PER_VOTE);
            return candidate.getVoteCount();
        } else {
            throw new RuntimeException("Candidat non trouvé");
        }
    }

    @Override
    public long getTotalGain() {
        return totalGain;
    }

    @Override
    public void incrementTotalGain(int amount) {
        totalGain += amount;
    }


    @Override
    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id).orElse(null);
    }

    @Override
    public Candidate getTopCandidateByGender(Gender gender) {
        return candidateRepository.findAll().stream()
                .filter(c -> c.getGender() == gender)
                .max(Comparator.comparing(Candidate::getVoteCount))
                .orElse(null);
    }
}
