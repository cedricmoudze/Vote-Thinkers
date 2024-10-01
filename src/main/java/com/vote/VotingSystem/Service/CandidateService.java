package com.vote.VotingSystem.Service;

import com.vote.VotingSystem.Model.Candidate;
import com.vote.VotingSystem.Model.Gender;

import java.util.List;

public interface CandidateService {

    /*methode to save candidate in database*/
    Candidate saveCandidate(Candidate candidate);

    /*delete candidate from database*/
    void deleteCandidate(Long id);

    /*get list of candidate to the view*/
    List<Candidate> getAllCandidates();

    /*count the number of vote for each candidate*/
    int incrementVoteCount(Long id);

    //incrementer le gain a chaque vote
    long getTotalGain();

    void incrementTotalGain(int amount);

    /*get a candidate by his id for operations*/
    Candidate getCandidateById(Long id);

    //obtenir les candidate en tete par genre
    Candidate getTopCandidateByGender(Gender gender);
}
