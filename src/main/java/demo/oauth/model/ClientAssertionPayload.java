package demo.oauth.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientAssertionPayload {

  public Long exp;

  public UUID jti;

  public String iss;

  public String aud;

  public String sub;
}
