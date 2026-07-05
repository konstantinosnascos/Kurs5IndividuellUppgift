OWASP TOP 10 säkerhetsanalys

Uppgift 2k5

hitta 3 risker:
A06
dependencies med kända sårbarheter. inga varnas för i pom, dubbelkolla med en extern tjänst
snabbt, kanske inte finns problem
Slog på dependency graph, dependabot alerts/security updates
<img src="images/dependabot.png" alt="Tester/Docker" width="600">


Repositoryt kollades med GitHub Dependabot och Secret Scanning. Dependabot hittade inga sårbarheter i projektets Maven-dependencies. Secret Scanning hittade inga exponerade hemligheter eller API-nycklar i repositoryt. 
Lagt till 
<groupId>org.owasp</groupId>
<artifactId>dependency-check-maven</artifactId>
i pom och kör en kontroll också just nu,

mvn dependency-check:check -DnvdApiKey=nist.api.key

A05
test-endpoint är kvar. kan lägga som dev-profil kanske
jag har kvar clientservice som inte används, kan tas bort,
snabbt

Lagt till @Profile("dev") i ratelimittestcontroller.
#spring.profiles.active=dev

A08 Security Misconfig
endast en config klass just nu, men den är väldigt simpel, hitta problem med vad den inte täcker och lägg till.

A04 unrestricted resource consumption, en del är hanterat. kolla vad som saknas och förbättra snabbt?

har lagt till bucket4j för att begränsa antal anrop man kan göra(del av lösning för A04)



$env:NVD_API_KEY="...b"
mkdir C:\temp
$env:TEMP="C:\temp"
$env:TMP="C:\temp"
./mvnw dependency-check:check



New-Item -ItemType Directory -Force C:\temp
$env:TEMP="C:\temp"
$env:TMP="C:\temp"
./mvnw dependency-check:check -DnvdApiKey=$env:NVD_API_KEY

Failed to request component-reports

Tror build fail är pga failar för många dependecy-checks och att den listar vad som måste åtgärdas.
Vissa saker kan man fixa i pom.xml tror jag, andra är jag osäker på exakt vad dom är ens.

<img src="images/nvd-build-fail.png" alt="nvd build fail konsol" width="600">


