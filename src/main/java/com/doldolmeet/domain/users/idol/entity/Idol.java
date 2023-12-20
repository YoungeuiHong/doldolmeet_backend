package com.doldolmeet.domain.users.idol.entity;

import com.doldolmeet.domain.capture.entity.Capture;
import com.doldolmeet.domain.commons.Role;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.commons.UserCommons;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Idol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Embedded
    private UserCommons userCommons;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @OneToMany(mappedBy = "idol", cascade = CascadeType.ALL)
    private List<Capture> captures = new ArrayList<>();

    private String teleRoomId;
    private String waitRoomId;

    public void createTeleRoomId() {
        this.teleRoomId = UUID.randomUUID().toString();
    }

    public void createWaitRoomId() {
        this.waitRoomId = UUID.randomUUID().toString();
    }

    public void setTeam(Team team) {
        this.team = team;
        team.getIdols().add(this);
    }

    public void setUserCommons(String username, String password, Role role) {
        this.userCommons.setUsername(username);
        this.userCommons.setPassword(password);
        this.userCommons.setRole(role);
    }
}
