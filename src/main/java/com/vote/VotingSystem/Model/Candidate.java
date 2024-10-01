package com.vote.VotingSystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullname;
    private String voteNumber;

    @Lob
    @Column(length = 1000, columnDefinition="LONGBLOB")
    private byte[] photo;

    private int voteCount;

    private double votePercentage;

    @Enumerated(EnumType.STRING)
    private Gender gender;
}
