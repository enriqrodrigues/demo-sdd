package br.com.demo.cadastro.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailsTeste {

    private static final Pattern TOKEN = Pattern.compile("/ativar\\?token=([A-Za-z0-9_-]+)");

    private EmailsTeste() {}

    public static MimeMessage aguardarMensagem(GreenMailExtension greenMail, String destinatario) {
        return await().atMost(Duration.ofSeconds(10))
                .until(() -> mensagensPara(greenMail, destinatario), lista -> lista.size() == 1)
                .get(0);
    }

    public static List<MimeMessage> mensagensPara(GreenMailExtension greenMail, String destinatario) {
        return Arrays.stream(greenMail.getReceivedMessages())
                .filter(m -> destinatarios(m).contains(destinatario))
                .toList();
    }

    public static String conteudo(MimeMessage mensagem) {
        try {
            return extrair(mensagem.getContent());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String extrairToken(MimeMessage mensagem) {
        Matcher m = TOKEN.matcher(conteudo(mensagem));
        assertThat(m.find()).as("link de ativação presente no e-mail").isTrue();
        return m.group(1);
    }

    private static List<String> destinatarios(MimeMessage mensagem) {
        try {
            return Arrays.stream(mensagem.getAllRecipients()).map(Address::toString).toList();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String extrair(Object conteudo) throws Exception {
        if (conteudo instanceof String texto) {
            return texto;
        }
        if (conteudo instanceof Multipart partes) {
            StringBuilder resultado = new StringBuilder();
            for (int i = 0; i < partes.getCount(); i++) {
                resultado.append(extrair(partes.getBodyPart(i).getContent())).append('\n');
            }
            return resultado.toString();
        }
        return "";
    }
}
