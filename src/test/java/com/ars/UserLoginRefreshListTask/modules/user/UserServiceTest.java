package com.ars.UserLoginRefreshListTask.modules.user;

import com.ars.UserLoginRefreshListTask.modules.user.dtos.CreateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UserResponse;
import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void create_deveCriarUsuarioComSenhaCriptografada_quandoEmailNaoExiste() {
        CreateUserDto dto = new CreateUserDto("Ana Silva", "ana@email.com", "senha123", "dev");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = userService.create(dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ana Silva");
        assertThat(response.email()).isEqualTo("ana@email.com");
        assertThat(response.description()).isEqualTo("dev");
        assertThat(saved.getPassword()).isNotEqualTo("senha123");
        assertThat(saved.getPassword()).startsWith("$2a$");
    }
}
