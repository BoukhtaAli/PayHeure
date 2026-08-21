package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PointageCreateRequest;
import com.example.payheurebackend.dto.PointageResponse;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.PointageMapperImpl;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointageServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PointageRepository pointageRepository;

    // Construit après l'injection des @Mock par MockitoExtension, voir EmployeeServiceImplTest.
    private PointageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PointageServiceImpl(employeeRepository, pointageRepository, new PointageMapperImpl());
    }

    @Test
    void creer_salarieExistantEtActif_enregistreLeBadgeage() {
        Employee amel = Employee.builder().id(1L).matricule("E001").nom("Boukhta").prenom("Amel").build();
        LocalDateTime dateHeure = LocalDateTime.of(2026, 1, 5, 8, 30);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(amel));
        when(pointageRepository.save(any())).thenAnswer(invocation -> {
            Pointage p = invocation.getArgument(0);
            p.setId(42L);
            return p;
        });

        PointageResponse response = service.creer(new PointageCreateRequest(1L, dateHeure));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.dateHeure()).isEqualTo(dateHeure);

        ArgumentCaptor<Pointage> captor = ArgumentCaptor.forClass(Pointage.class);
        verify(pointageRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployee()).isEqualTo(amel);
        assertThat(captor.getValue().getDateHeure()).isEqualTo(dateHeure);
    }

    @Test
    void creer_salarieInexistant_leveResourceNotFoundExceptionSansEnregistrer() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(new PointageCreateRequest(99L, LocalDateTime.now())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aucun salarié avec l'identifiant 99");

        verifyNoInteractions(pointageRepository);
    }

    @Test
    void creer_salarieSupprimeLogiquement_leveResourceNotFoundExceptionSansEnregistrer() {
        Employee deleted = Employee.builder().id(2L).matricule("E002").nom("El Amrani").prenom("Youssef").build();
        deleted.markDeleted();
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.creer(new PointageCreateRequest(2L, LocalDateTime.now())))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(pointageRepository);
    }
}
