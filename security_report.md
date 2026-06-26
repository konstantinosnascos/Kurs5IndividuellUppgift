hitta 3 risker:
A01 eller A07
Ingen auth som begränsar vem som kan använda appen eller hur mycket tokens den personen kan använda.
-kommer ta lång tid


A06
dependencies med kända sårbarheter. inga varnas för i pom, dubbelkolla med en extern tjänst
snabbt, kanske inte finns problem
Slog på dependency graph, dependabot alerts/security updates

Repositoryt kollades med GitHub Dependabot och Secret Scanning. Dependabot hittade inga sårbarheter i projektets Maven-dependencies. Secret Scanning hittade inga exponerade hemligheter eller API-nycklar i repositoryt. 
Lagt till 
<groupId>org.owasp</groupId>
<artifactId>dependency-check-maven</artifactId>
i pom och kör en kontroll också just nu,


A05
test-endpoint är kvar. kan lägga som dev-profil kanske
jag har kvar clientservice som inte används, kan tas bort,
snabbt

tidigare skrivit om som kan gå snabbt:
A01 (BOLA) -nej, mycket kod att först implementera användare

A08 Security Misconfig
endast en config klass just nu, men den är väldigt simpel, hitta problem med vad den inte täcker och lägg till.

A04 unrestricted resource consumption, en del är hanterat. kolla vad som saknas och förbättra snabbt?

