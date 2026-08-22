package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.dto.EmployeeResponse;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import com.example.payheurebackend.dto.PageResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.EmployeeMapperImpl;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le mapper est l'implémentation MapStruct réelle (pas un mock) : ça exerce sa logique en même
 * temps que celle du service, plutôt que de la retester séparément pour rien.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PointageRepository pointageRepository;

    // Construit après l'injection des @Mock par MockitoExtension : un initialiseur de champ
    // s'exécuterait avant (les champs @Mock ne sont peuplés qu'après la construction de
    // l'instance de test), et employeeRepository y serait encore null.
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(employeeRepository, pointageRepository, new EmployeeMapperImpl(), new PointageSessionAssembler());
    }

    private static Employee employee(long id, String matricule, String nom, String prenom) {
        return Employee.builder().id(id).matricule(matricule).nom(nom).prenom(prenom).build();
    }

    @Test
    void search_periodeIncoherente_leveInvalidPeriodExceptionSansAppelerLeRepository() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 10, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 1, 0, 0);
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(null, debut, fin, null);

        assertThatThrownBy(() -> service.search(criteria, PageRequest.of(0, 10)))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessage("La date de fin ne peut pas être antérieure à la date de début");

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void search_appliqueLeTriParNomPuisPrenomEtMappeLaPage() {
        Employee amel = employee(1L, "E001", "Boukhta", "Amel");
        Page<Employee> page = new PageImpl<>(List.of(amel), PageRequest.of(0, 10), 1);
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeResponse> result = service.search(new EmployeeSearchCriteria(null), PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).matricule()).isEqualTo("E001");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "nom", "prenom"));
    }

    @Test
    void findById_salarieExistantEtActif_retourneLeSalarieMappe() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L, "E001", "Boukhta", "Amel")));

        EmployeeResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nom()).isEqualTo("Boukhta");
        assertThat(response.deleted()).isFalse();
    }

    @Test
    void findById_salarieInexistant_leveResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aucun salarié avec l'identifiant 99");
    }

    @Test
    void findById_salarieSupprimeLogiquement_leveResourceNotFoundException() {
        Employee deleted = employee(2L, "E002", "El Amrani", "Youssef");
        deleted.markDeleted();
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.findById(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aucun salarié avec l'identifiant 2");
    }
}
