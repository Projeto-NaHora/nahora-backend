package com.nahora.repositories;

import com.nahora.model.Profissional;
import com.nahora.model.enums.StatusVerificacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProfissionalRepositoryTest {

    @Autowired
    private ProfissionalRepository profissionalRepository;

    // ---- save ----

    @Test
    @DisplayName("Deve salvar profissional com listas de especialidades e áreas de atuação com sucesso")
    void save_SucessoComListasPreenchidas() {
        // Arrange
        Profissional profissional = new Profissional();
        
        // Dados de Usuario 
        profissional.setNome("Guilherme Profissional");
        profissional.setEmail("guilherme.pro@email.com");
        profissional.setTelefone("81988888888");
        profissional.setSenha("SenhaForte123");
        profissional.setAtivo(true);
        
        // Dados de Profissional
        profissional.setCpf("12345678909");
        profissional.setAnosExperiencia(5);
        profissional.setValorInicial(new BigDecimal("150.00"));
        profissional.setStatusVerificacao(StatusVerificacao.NAO_ENVIADO); 
        profissional.setPerfilCompleto(false);

       
        profissional.setAreaAtuacao(List.of("Recife", "Olinda"));
        profissional.setEspecialidades(List.of("Encanador", "Eletricista"));

  
        Profissional salvo = profissionalRepository.save(profissional);

        
        assertNotNull(salvo, "O objeto salvo não deveria ser nulo");
        assertNotNull(salvo.getId(), "O ID não deveria ser nulo após salvar no banco");
        assertEquals("12345678909", salvo.getCpf());
        
        // Verificando se as tabelas auxiliares funcionaram
        assertNotNull(salvo.getAreaAtuacao());
        assertEquals(2, salvo.getAreaAtuacao().size(), "Deveria ter salvo 2 áreas de atuação");
        assertTrue(salvo.getAreaAtuacao().contains("Recife"));
        
        assertNotNull(salvo.getEspecialidades());
        assertEquals(2, salvo.getEspecialidades().size(), "Deveria ter salvo 2 especialidades");
        assertTrue(salvo.getEspecialidades().contains("Eletricista"));
    }

    // ---- findById ----

    @Test
    @DisplayName("Deve recuperar um profissional salvo e carregar suas listas corretamente")
    void findById_Sucesso() {
        // Arrange
        Profissional profissional = new Profissional();
        profissional.setNome("Carlos Silva");
        profissional.setEmail("carlos@email.com");
        profissional.setTelefone("81977777777");
        profissional.setSenha("Senha123");
        profissional.setCpf("98765432100");
        profissional.setEspecialidades(List.of("Marceneiro"));
        
        Profissional salvo = profissionalRepository.save(profissional);

        // Act
        Profissional recuperado = profissionalRepository.findById(salvo.getId()).orElse(null);

        // Assert
        assertNotNull(recuperado);
        assertEquals("Carlos Silva", recuperado.getNome());
        assertEquals(1, recuperado.getEspecialidades().size());
        assertEquals("Marceneiro", recuperado.getEspecialidades().get(0));
    }
}