# Forskningsmöjligheter — Verdant + Blomsterportalen

*Intern visionstext. Huvudsyfte: kristallisera vad en sammanhängande Verdant- och Blomsterportalen-plattform öppnar för forskarvärlden. Sekundärt syfte: fungera som en meny som vid behov kan brytas ut och skickas till akademiska partners.*

---

## Varför det här datasetet är forskningsintressant

De flesta jordbruks- och livsmedelsplattformar sitter på data som är antingen (a) *djup men smal* — en enskild producent har hög upplösning på sin egen verksamhet men inget att jämföra med — eller (b) *bred men grund* — en marknadsplats har transaktioner men inte det biologiska, agronomiska och platsspecifika lagret som föregår dem. Kombinationen Verdant + Blomsterportalen ligger på en nästan obesatt punkt i det rummet: en longitudinell, radnivå-upplöst, BankID-förankrad koppling mellan *hur en gröda odlas* (Verdant: bädd, sort, händelsehistorik, kvalitet) och *hur den säljs, skickas och konsumeras* (Blomsterportalen: listning, pris, substitution, logistik, faktura).

Det är den kopplingen — odling-till-konsument på radnivå, med samtyckesgraderad identitet — som gör att frågor som tidigare måste besvaras genom enkäter, skattningar eller proxyvariabler nu kan besvaras direkt. Den här texten försöker kartlägga *vilka* frågor.

### Antaganden om data

Texten antar ett **2–4-årsfönster** där dagens Verdant- och Blomsterportalen-modeller fått några års historia och kompletterats med tre blygsamma tillägg som är tekniskt realistiska: (1) daglig väderdataintegration (SMHI) kopplad till `Garden`-geolokation, (2) strukturerad post-leverans-återkoppling från florister (vaslivsutfall, kvalitetsklagomål på radnivå), och (3) opt-in konsumentkvitton när B2C-spåret öppnar. Där en tråd kräver mer än så markeras det explicit. En kort coda längst ner pekar på spekulativa 5+-årshorisonter (pollinatörkameror, drönarbilder, biobank-kopplad genomik).

---

## Strategisk motivering — varför vi vill möjliggöra det här

Att stå värd för forskning är inte välgörenhet. Det finns fyra konkreta skäl till att det är i produktens intresse:

1. **Datanätverkseffekter.** Varje ny producent stärker värdet av datasetet för alla andra användare — både kommersiellt (bättre prognoser, smartare substitution) och forskningsmässigt. Ett uttryckligt forskningsspår gör den dynamiken synlig och förhandlingsbar.
2. **Försvarbarhet.** En konkurrent kan kopiera UI och prismodell. Ett samtyckesgraderat, longitudinellt, radnivå-kopplat dataset byggt över åren är svårare att replikera än något annat i stacken.
3. **Samfinansiering.** Vinnova, Formas, Horizon Europe, och EU:s gemensamma jordbrukspolitiks forsknings- och innovationsspår (EIP-AGRI) finansierar gärna plattformar som levererar datatillgång. En existerande forskningsplan snabbar upp ansökningstiden från månader till veckor.
4. **Producentlojalitet.** En odlare som får tillbaka benchmarkdata, får sitt arbete räknas in i publicerad forskning, och ser sitt namn som medförfattare har högre "switching cost" än en som bara får en faktura.

Sammantaget: forskning är inte ett sidoprojekt utan ett sätt att göra plattformen tyngre, mer finansierad och mer meningsfull att stanna kvar på.

---

## 1. Klimat & miljö

Snittblomsodling är klimatpolitiskt underskattat. Sverige importerar den absoluta majoriteten av sina snittblommor — främst från Nederländerna, sekundärt från Kenya och Etiopien — vilket innebär växthus- och flygburen försörjning av en produkt som i grunden inte har någon kalorisk eller strukturell nytta. Det gör sektorn både till ett ovanligt känsligt klimatmål (flyget kan rimligen inte fortsätta) och till en av de enklaste att flytta lokalt. Verdant + Blomsterportalen sitter mitt i den omställningen.

### 1.1 Klimatnytta genom importsubstitution: livscykelanalys på radnivå

**Hypotes:** Den marginella klimatnyttan av att ersätta en nederländsk eller kenyansk snittblomma med en svenskodlad motsvarighet, uttryckt i kg CO₂e per såld stjälk, är större och mer heterogen än vad befintliga översiktsstudier (Williams 2007, Franze & Ciroth 2011) antyder — och variationen förklaras i hög grad av just de variabler Blomsterportalen redan fångar: ursprungsnav, leveranslegg, kylkedjeklass, substitutionstier och säsong.

**Vad som gör den här studien möjlig, och unik.** LCA på snittblommor är historiskt baserat på modellerade genomsnitt: en hypotetisk ros, ett hypotetiskt växthus, ett hypotetiskt flygplan. Blomsterportalen har faktiska `Shipment`-leggar, faktiska `TemperatureTrace`-serier, faktiska `InvoiceLine`-rader med ursprung, och en förbrukningsmodell för växthusenergi som kan härledas från Verdants `Plant Event`-historik (såddatum, pottdatum, eventuell potplacering inomhus) tillsammans med `Garden`-geolokation och väderdata. Kombinationen tillåter LCA *per sålt knippe* — inte per genomsnittlig blomsterstjälk — vilket gör det möjligt att besvara politiskt laddade frågor som *"är svenskodlad dahlia i oktober bättre än importerad i maj?"* med riktiga siffror.

**Tråden sträcker sig vidare.** Samma datastruktur ger också en knivskarp utvärderingsgrund för klimatmärkningar (`Svenskodlat`, `KRAV`) — man kan mäta den faktiska CO₂-skillnaden mellan certifierade och icke-certifierade flöden, inte bara påståenden. Kandidatpartners: SEI (Stockholm Environment Institute, LCA-metodik), RISE (tillämpad LCA), SLU Alnarp (trädgårdsproduktion), Lund (ekonomiska drivkrafter för substitution). Metodhint: hybrid LCA med processdata från Verdant/BP + IO-data för uppströms-insatser, Monte Carlo för osäkerhet.

### 1.2 Fenologisk förskjutning i nordisk snittblomsodling

**Hypotes:** Blomningsdatum för svenskodlade snittblommor har redan förskjutits flera dagar till en vecka senare i den faktiska odlarpraktiken jämfört med vad klimatdriven modellfenologi skulle förutsäga — eftersom odlare adapterar sortval och såddatum i respons till både klimat och marknad. Att mäta den skillnaden ger den första empiriska siffran på *agronomisk anpassningshastighet* i Nordeuropeisk hortikultur.

**Datan som möjliggör detta.** Verdants `Plant Event`-modell fångar redan datum för sådd, utplantering, knoppning, blomning och skörd per bädd. Med multiårig historik, geolokation och inkopplad SMHI-data får man en longitudinell, regionalt upplöst, sortspecifik fenologiserie — något som i praktiken inte finns idag för prydnadsgrödor i Norden (akademisk fenologi fokuserar på träd och grödor av livsmedelskaraktär). `Season`-modellens frostdata och `Species.daysToSprout/daysToHarvest` ger en baslinje att jämföra faktisk utveckling mot.

**Varför det är publicerbart.** Fenologisk forskning lider av att observationsserier är (a) korta, (b) begränsade till amatörregistreringar (fågel- och botanikföreningar), eller (c) koncentrerade till forskningsträdgårdar med icke-representativa odlingsförhållanden. Ett dataset där flera hundra professionella odlare spridda över Sveriges klimatzoner registrerar händelser som en naturlig del av sitt arbete är metodologiskt guld. Kandidatpartner: SLU Alnarp (hortikultur), SMHI (klimatdata), Lunds universitet (fenologisk ekologi), Svenska fenologinätverket. Metodhint: hierarkiska blandade modeller med sort × zon × år; tillväxtgradsdagar som mediator.

### 1.3 Pollinatörer och biodiversitet på bädd-nivå

**Hypotes:** Småskalig snittblomsodling representerar ett extremt pollinatörs­attraherande grödekomplex — med blomrikedom per ytenhet som sannolikt överstiger alla andra kommersiella grödor i svensk jordbrukslandskap — och utgör därmed en underutforskad bidragande faktor till pollinatörsnätverks resiliens i rural-urbana övergångszoner. Hypotesen är testbar: om snittblomsbeddar fungerar som näringskällor, bör pollinatörstäthet och artsammansättning korrelera med sortmixens blomningsöverlapp, bäddtäthet per hektar, och avstånd till semi-naturliga habitat.

**Vad Verdant bidrar med som inte finns idag.** Landskapsekologisk forskning på pollinatörer — framför allt vid CEC Lund, där Henrik Smith och kollegor arbetat med landskap × biodiversitet i över ett decennium — har varit beroende av grov grödkartering från Jordbruksverkets blockdatabas. Verdants `Bed`-modell med per-bädd sortidentitet, blomningsfönster från `Plant Event`, och `Garden`-koordinater erbjuder en upplösning som Jordbruksverkets data inte kommer i närheten av. Med hundratals odlare kartlagda blir det möjligt att studera *designval* som behandling: vad händer med pollinatörsförekomst om en odlare skiftar sortmix mitt i säsongen?

**Vad som krävs och vad det öppnar.** Studien kräver antingen en frivillig pollinatörsobservationsmodul i Verdant-appen (enkel bildinsamling med ML-artidentifiering är rimligt tekniskt) eller partnerskap med Artportalen så att geotaggade observationer kan joinas mot bäddmatriser. Kandidatpartner: CEC Lund, Artdatabanken vid SLU, BECC-programmet (Biodiversity and Ecosystem services in a Changing Climate). Metodhint: landskapsekologiska responsmodeller med bäddsammansättning som predictor; spatial autokorrelation på gård- och landskapsskala.

### 1.4 Cirkulär ekonomi i snittblomskedjan

**Hypotes:** Den samlade cirkulariteten i svensk snittblomskedja — från fröförpackning och krukmaterial genom transportbehållare (CC-containrar, bucket pool) till postkonsumentavfall (kompost, brännbart) — är både mätbar och aktivt påverkbar via plattformsdesign, men har aldrig kvantifierats eftersom ingen aktör hittills haft vare sig överblick eller datastruktur för det.

**Datan som möjliggör mätningen.** Blomsterportalens `AssetPool`-modell (CC-containrar, hinkar) kombinerat med radnivå-`ShipmentLeg` och `Manifest` ger fullständig spårbarhet för återanvändbara enheter: hur många rundor gör en genomsnittlig hink innan den tas ur bruk, var i nätverket förluster sker, och vad det kostar i ersättningsinköp. Lägger man till Verdants insatsdata (frökällor, koldelar från tidigare skördar som återvinns på egna bäddar) får man en närmast fullständig cirkulationsmatris för sektorn — något som Eurostat och EU-kommissionens arbete med digitala produktpass aktivt letar efter referensdata till.

**Varför det är politiskt aktuellt.** EU:s Ecodesign for Sustainable Products Regulation (ESPR) kommer att kräva digitala produktpass för allt fler produktkategorier, och snittblommor ligger i praktiken i kön. En etablerad dataplattform för cirkularitetsmätning skulle göra Verdant+BP till naturlig referensimplementation. Kandidatpartner: IVL Svenska Miljöinstitutet, RISE, Chalmers (Division of Environmental Systems Analysis). Metodhint: Material Flow Analysis (MFA) med fallerings- och återvinningsrater estimerade från `AssetPool`-eventhistorik.

---

## 2. Människor & försörjning

Små producenter av snittblommor är en av de mest osynliga grupperna i svensk landsbygdsekonomi: för små för jordbruksstatistiken, för spridda för branschorganisationer, och för nya för traditionella forskningskohorter. Plattformsdata gör dem för första gången systematiskt observerbara — på ett sätt som också respekterar deras integritet.

### 2.1 Landsbygdsutveckling och den nya småbruksekonomin

**Hypotes:** Småskalig snittblomsodling — ett segment som knappast existerade i Sverige före 2015 — har vuxit snabbast i kommuner med kombinationen (a) pendlingsavstånd till tätort, (b) nedläggning av traditionell animalisk produktion, och (c) lokal florist- och marknadsinfrastruktur. Plattformen kan för första gången visa den geografiska fördelningen av denna "nya småbruksvåg" i realtid.

**Datan och metoden.** Blomsterportalens `Organization`-data (Bolagsverket-registrering, KYB-data från Roaring.io), kombinerat med `Payout`-serier och Verdant-aktivitet per `Garden`, ger en direkt mätning av var nyetablering sker, hur livslängden ser ut, och vilka försörjningsnivåer som är möjliga. Eventhistoryanalys för verksamhetsöverlevnad och geografiskt viktade regressioner för platsfaktorer är lämpliga verktyg. Kopplingen till Jordbruksverkets strukturregister — via organisationsnummer — gör det möjligt att placera snittblomssektorn i ett större rural-ekonomiskt sammanhang, trots att den idag faller under de flesta officiella tröskelvärdena.

**Varför det är policyrelevant.** Svensk landsbygdspolitik och CAP-implementering utgår fortfarande från en jordbruksmodell där snittblommor är osynliga. Samtidigt är detta en av de få sektorer där nya jordbrukare (ofta unga, ofta kvinnor, ofta utan agronomisk familjebakgrund) faktiskt etablerar sig. Det är en empirisk lucka med direkta politiska implikationer — och en tråd som sannolikt skulle kunna samfinansieras via Jordbruksverket, Länsstyrelsen eller det EU-stödda EIP-AGRI-nätverket. Kandidatpartner: SLU institutionen för stad och land, SLU Future Food, Jordbruksverkets analysenhet.

### 2.2 Odlares välbefinnande och försörjning via longitudinell BankID-kohort

**Hypotes:** Odlares subjektiva välbefinnande över tid är starkare kopplat till *förutsägbarheten* i efterfrågan (cykelstabilitet, substitutionsfrekvens, sena avbeställningar) än till nettopayout-nivån. Om det stämmer har det direkta implikationer för både plattformsdesign och jordbrukspolitik.

**Den metodologiska möjligheten.** Det här är tråden där BankID-identiteten i Blomsterportalen blir forskningsmoat snarare än bara compliancefråga. Genom frivillig opt-in kan en odlarkohort upprätta ett longitudinellt samtycke för att koppla plattformsdata (arbetsbelastning via Verdants `Plant Event`-frekvens, försörjning via BP:s `Payout`-serier, marknadsrelationer via `ReliabilityScore`-historik) till enkätbaserade utfallsmått på mental hälsa, upplevd autonomi och yrkesidentitet. Motsvarande kopplingar är idag i princip omöjliga att göra utanför registerforskning — och registerforskning kan inte observera *plattformsbeteende*. Kombinationen är unik.

**Varför det spelar roll för produkten.** Om hypotesen stämmer blir argumentet för att investera i cykelstabilitet (minska sena avbeställningar, förbättra substitutionsprediktion) inte ett UX-argument utan ett folkhälsoargument — vilket i sin tur öppnar finansieringskällor som annars är stängda. Kandidatpartner: Karolinska Institutets IMM, Stressforskningsinstitutet vid Stockholms universitet, Gothenburg Research Institute (plattformsekonomi), SLU rural sociology. Metodhint: longitudinella latenta tillväxtkurvor; inom-person fixed effects för att identifiera förutsägbarhetseffekter separat från nivåeffekter.

### 2.3 Genus, succession och vem som odlar blommor

**Hypotes:** Svensk småskalig snittblomsodling är — helt i motsats till traditionellt jordbruk — statistiskt kvinnodominerad, och den dominansen är inte slumpmässig utan strukturerad av (a) vilka kanaler människor tar sig in i yrket genom, (b) arbetsfördelning inom samägda verksamheter, och (c) hur generationsväxling skiljer sig från familjelantbruksmodellens övergångsmönster. Att kvantifiera mönstret och dess drivkrafter skulle utgöra ett unikt empiriskt bidrag till nordisk genusforskning om jordbruk.

**Datan.** `Membership.role`-strukturen i Blomsterportalen tillsammans med användarmetadata (BankID ger verifierat kön och ålder, om samtycke finns för den användningen) gör det möjligt att kartlägga rollfördelning per kön, ålder, företagsstorlek, och region över tid. Särskilt intressant är dynamiken inom flera-personers-organisationer: vem är registrerad som `owner`, vem som `listing_manager`, vem som `packer` — och hur förändras den fördelningen när ett par driver verksamheten tillsammans?

**Varför det är viktigt.** Den maskulina dominansen i svenskt jordbruk har varit föremål för betydande forskning, men snittblomssektorn som motexempel har knappast studerats. Kandidatpartner: Göteborgs universitet (genusvetenskap), SLU institutionen för stad och land, NORA (Nordic Association for Women's Studies and Gender Research). Kräver tillägg: frivillig demografisk opt-in-registrering. Metodhint: komparativ kvantitativ analys mot LRF:s medlemsdata och Jordbruksverkets strukturregister, kompletterad med djupintervjuer för den kvalitativa dimensionen.

---

## 3. Konsument och välbefinnande

### 3.1 Biophilia, gåvokultur och affektiv konsumtion

**Hypotes:** Blomköp är ett av de mest affektivt laddade och samtidigt minst empiriskt studerade konsumtionsmönstren i svensk handel. Köpbeslutet drivs av en komplex blandning av humörreglering (för mig själv), social signalering (för någon annan), och estetisk preferens — en blandning som i existerande litteratur rekonstruerats via enkäter och laboratorieexperiment snarare än observerad transaktionsdata. Vid öppnandet av B2C-spåret kommer Blomsterportalen att ha ett av Europas tätaste observationsfönster mot dessa mekanismer.

**Vad datan tillåter.** Med samtyckesgraderade konsumentkvitton kan man koppla *vem* som köper *vad* åt *vem* (gåva vs. egen konsumtion, via leveransadress) *när* (säsong, vardagar vs. helger, närhet till livshändelser) till radnivå-produktegenskaper (sort, ursprung, certifiering, pris). Det öppnar för frågor som: I vilken utsträckning drivs blomköp av humörreglering kontra social signalering? Hur skiljer sig gåvomönster mellan åldrar, livshändelser och geografiska kontexter? Påverkar svenskhetsmärkning (`Svenskodlat`, `KRAV`) det upplevda värdet och återköpsfrekvensen — och i så fall för vilka konsumentsegment?

**Identifikation och partners.** Kausal identifikation av emotionell elasticitet kräver exogena chocker; leveransförseningar orsakade av väder eller logistikstörningar fungerar som instrument. Kandidatpartner: Handelshögskolan i Stockholm (House of Innovation, Center for Retailing), Centrum för konsumtionsforskning vid Göteborgs universitet, Karolinska Institutet (affektiv psykologi). Metodhint: instrumentvariabel-approach, segmenterad diff-in-diff för märkningseffekter, hierarkisk modellering för hushållsnivå-variation.

### 3.2 Sorg- och minneskonomi

**Hypotes:** Blommor vid begravning, årsdagar, jubileer och andra minnesögonblick utgör en ekonomiskt betydande men kvantitativt nästan osynlig del av svensk snittblomskonsumtion. Denna "minneskonomi" har emotionell och kulturell laddning som gör den svår att studera med konventionella metoder — respondenter är obekväma, konsumenter minns felaktigt, och transaktionsdata saknas. Just de egenskaperna gör tråden till en av de mest värdefulla för forskare som får tillgång till den här plattformen.

**Datan.** Till skillnad från bröllops- eller eventmarknaden — där stora belopp ändå hanteras relativt synligt — är sorgekonomin fragmenterad: enskilda konsumenter, små florister, oregelbundna transaktioner, ofta korta ledtider. Blomsterportalens orderdata (sort, mängd, pris, leveransdatum, mottagardress) kombinerat med florist-kategorisering (`delivery_type`, `order_note` där samtycke finns) och säsongsvariation ger för första gången en struktur­erad observationsram. Longitudinell koppling till konsumenter som återkommer årsvis vid samma datum (minnesdagar, årsdagar) är metodologiskt kraftfull.

**Varför det är värdefullt.** Forskning om sorg, minne och konsumtion är en aktiv skärningspunkt mellan konsumentsociologi, death studies och kulturekonomi, men systematisk data saknas. Kandidatpartner: Centrum för konsumtionsforskning vid Göteborgs universitet, Uppsala universitets center för forskning om religion och samhälle (där forskning om ritualer och materiell kultur bedrivs), Lund (kulturvetenskap). Etiskt känsligt — särskild varsamhet kring samtycke krävs. Metodhint: survival analysis för återbesöksintervall; kvalitativ komplettering via djupintervjuer med samtyckande konsumenter.

---

## 4. Agronomi och produktionsvetenskap

### 4.1 Federerad sortprovning

**Hypotes:** Ett federerat sortprovningsdataset från flera hundra svenska producenter över flera säsonger kommer att visa att genotyp × miljö × odlarpraktik-interaktionen är betydligt större än den som syns i traditionella provparcellsdata — och att sort­rekommendationer från auktoriserade källor (SLU Alnarp, branschrådgivning) systematiskt förbiser sorter som presterar utmärkt i vissa odlarpraktiker men underpresterar i provparcellsmiljö.

**Datan.** Verdants `VarietyTrial`-entitet registrerar redan sida-vid-sida-jämförelser per säsong med kvantitativa utfall: antal stjälkar, stjälklängd, vaslivslängd, kvalitetsbedömning, kundrespons, och slutlig verdict (Keep/Expand/Reduce/Drop/Undecided). Kombinerat med `Plant Event`-historik för odlingspraktik (såddatum, utplanteringsdatum, substrattyp om modellerat) och `Bed`-geografi utgör federationen en datastruktur som är kvalitativt annorlunda från traditionella provparceller: den fångar *faktisk kommersiell prestanda* under *faktisk skötsel* över *faktiska klimatzoner*.

**Varför det är betydelsefullt.** Växtförädling för snittblomssortiment i Nordeuropa är idag dominerad av nederländska och tyska förädlingshus vars provparceller ligger söder om våra klimatzoner. Ett nordiskt sortdataset av denna skala skulle direkt påverka rekommendationer, och på längre sikt kunna mata tillbaka in i förädlingsarbetet vid NordGen. Kandidatpartner: SLU Alnarp hortikultur, NordGen, LRF Trädgård. Metodhint: blandade modeller för genotyp × miljö × odlare; Bayesiansk partial pooling för få observationer per sort hos enskilda odlare; multitrait-analys för kvalitet–vaslivslängd–avkastning som korrelerade utfall.

### 4.2 Post-harvest-vetenskap och realism i vaslivslöften

**Hypotes:** Det finns en systematisk och mätbar gap mellan deklarerad vaslivslängd (`Species.vaseLifeDays` i Verdant, upprepad i Blomsterportalens produktlistningar) och faktisk vaslivslängd hos slutkonsument — och den gapens storlek är en funktion av mätbara faktorer: sort, skördestadium, kylkedjetemperaturprofil, antal leggar i transportkedjan, och tid-till-slutkund.

**Datan.** Post-harvest-forskning har länge identifierat kylkedjebrott som huvudfaktor i vaslivslängdens reduktion, men har saknat storskaliga dataset som kan kvantifiera effekten under verkliga distributionsförhållanden. Verdants skördedata (skördedatum, kvalitetsgrad, stjälklängd, och — i plant events — tidpunkt på dygnet) kombinerat med Blomsterportalens `TemperatureTrace` per `ShipmentLeg` och strukturerad florist-återkoppling på realiserad vaslivslängd (förutsätter ett tillägg som ändå ligger naturligt i produktens utveckling) ger för första gången ett komplett kedjedataset.

**Vad det möjliggör och vilka det intresserar.** En kalibrerad modell för *förväntad* vaslivslängd per produkt-till-kund-väg skulle låta Blomsterportalen sätta realistiska löften till konsument och upptäcka kylkedjeproblem i realtid. För forskningen öppnar det möjligheter som normalt kräver stora laboratorieexperiment: dos-respons-kurvor för temperaturavvikelser, interaktion mellan sort och kedjestress, och validering av etylenskyddsstrategier i verklig distribution. Kandidatpartner: SLU Alnarp post-harvest physiology, Aarhus University Department of Food Science, Wageningen University (för nederländsk kontext). Metodhint: överlevnadsanalys med konkurrerande risker; kylkedjebrott som tidsberoende kovariat; strukturella ekvationsmodeller för att separera sortlatenta egenskaper från kedjeeffekter.

---

## 5. Marknader och system

### 5.1 Substitutionskaskaden som naturligt experiment

**Hypotes:** När Blomsterportalens substitutionsmotor ersätter en beställd produkt med en annan enligt förkonfigurerad floristpolicy, utgör utfallet ett kvasi-randomiserat behandlingsfenomen som kan användas för att identifiera kausal efterfrågerespons — något som är mycket svårt att få i marknadsplatsdata där substitutioner normalt är endogena (floristen väljer själv).

**Varför det är ovanligt.** Kausalekonomisk identifikation av substitutionselasticiteter bygger oftast på svagt exogena instrument (prisförändringar från en tredjepartskälla, skattechocker, säsongsskift). Blomsterportalens sexstegsfallback — där tier-tilldelningen styrs av tillgängligheten hos andra odlare i nätverket, inte av florists eget val — ger något mycket närmare riktig exogenitet. Varje event lagras i `domain_events` med komplett attribution (producent, sort, tier, pris, distansökning), vilket innebär att man kan rekonstruera kontrafaktuella utfall med acceptabel statistisk kraft redan efter några tusen substitutioner.

**Publicerbarhet.** Plattformsekonomi som disciplin har i flera år letat efter datasets där substitutionsmekanik är (a) transparent, (b) kvasi-randomiserad, och (c) kopplad till verkligt konsumentutfall. Blomsterportalen kan leverera alla tre. Kandidatpartner: Handelshögskolan i Stockholm, IFN (Institutet för näringslivsforskning), Toulouse School of Economics (digital marknadsforskning), Göteborgs universitet (industriell organisation). Metodhint: regression diskontinuitet vid tier-tröskelvärden; instrument baserade på nätverks-tillgänglighet snarare än pris.

### 5.2 Försörjningskedjans resiliens

**Hypotes:** Ett småskaligt, distribuerat, multihubsbaserat logistiknätverk för färskvara — vilket är precis vad Blomsterportalens hub-och-spoke-topologi utgör — är en unikt lämplig testbädd för försörjningskedjeresiliens under klimat- och krisscenarier. Svensk civil beredskapsforskning letar efter just sådana observerbara miniatyrsystem.

**Datan.** Trenivås-topologin (`HubGraph`: leaf → local → regional) tillsammans med `ShipmentIncident`-registret, `TemperatureTrace`-serier och `ReliabilityScore` per producent och rutt ger fullständig observabilitet av hur chocker propagerar: vad händer när en regional hub får strömavbrott, när vägar är oframkomliga efter storm, när en nyckelproducent blir sjuk? Varje sådan händelse är i praktiken ett naturligt experiment, och den händelsesourcade arkitekturen gör det möjligt att rekonstruera systemets tillstånd vid godtyckliga tidpunkter.

**Varför det är relevant bortom snittblommor.** Forskning om resiliens i livsmedelskedjor har länge begränsats av att aktörerna är stora, svårobserverbara och politiskt känsliga. En snittblomsplattform av den här strukturen delar många egenskaper med lokala livsmedelskedjor (färskvara, kort ledtid, hub-och-spoke) men är avsevärt mer forskningsvänlig. Kandidatpartner: FOI (Totalförsvarets forskningsinstitut), MSB:s forskningsråd, RISE (kritisk infrastruktur), KTH (Industrial Engineering and Management). Metodhint: nätverksanalys med cascadingfel-simulering; empirisk validering mot de observerade incidenterna i historiken.

### 5.3 Prissättning, marknadsdesign och substitutionsekonomi

**Hypotes:** Blomsterportalens tvåveckorscykel med fasta cutoff-tider, `StandingPriceList` som producent-satt referens, RFQ-spår parallellt, och transparent avgiftsstruktur (10 % producentkommission, 1,5 % floristavgift) utgör en välavgränsad testbädd för klassiska frågor inom marknadsdesign som normalt är svåra att studera empiriskt: prisupptäckt vid tunn likviditet, reservationsvärden hos småskaliga säljare, och välfärdsimplikationer av olika allokeringsmekanismer.

**Unika datafördelar.** Standing price lists låter oss observera säljares deklarerade reservationspriser *innan* transaktion, vilket är ovanligt — i de flesta marknadsplatser är priserna dynamiska och endogena. Kombinationen med substitutionsmotorns tvingade matchningar ger en andra källa till exogen prisvariation. Tvåveckorscykelns fasta tempo (Sunday 24:00 → Tuesday, Wednesday 24:00 → Friday) skapar en regelbunden struktur som förenklar paneldataanalys på sätt som ad hoc-marknadsplatser inte gör.

**Frågor och partners.** Hur reagerar småskaliga producenter på tunn likviditet? I vilken grad skiljer sig RFQ-spårets allokeringseffektivitet från cykelspårets? Ger transparenta kommissionsnivåer andra beteenden än dolda marginaler (jämförbart mot andra marknadsplatser)? Kandidatpartner: Handelshögskolan i Stockholm (marknadsdesigngrupp), IFN, Jönköping International Business School, Toulouse School of Economics. Metodhint: strukturell estimering av bud- och reservationsvärdesfördelningar; välfärdsanalys via simulering av alternativa mekanismer; jämförande analys mot historiska svenska jordbruksauktioner.

---

## Forskningsgovernance — hur vi gör det här utan att förlora vår själ

Att möjliggöra forskning utan en tydlig governancestruktur är ett sätt att bränna producenternas och floristernas förtroende. Minimikraven:

1. **Samtyckesmodell.** BankID-autentiseringen ger oss den juridiska grunden att fråga individer om utvidgat samtycke för forskningsanvändning. Samtycke ska vara granulärt (per forskningsområde eller studie, inte ett enda klumpsamtycke), återkallbart, och loggat.
2. **Datatiers.**
   - **Tier 1 — aggregerat, öppet:** benchmarkdata, branschrapporter, årsöversikter. Ingen radnivå.
   - **Tier 2 — radnivå, akademisk, under DUA:** pseudonymiserade dataset för akreditrade forskare, bundet till data-use agreement, icke-kommersiellt.
   - **Tier 3 — samtyckesgradad identitet:** endast för specifika studier med explicit individuellt samtycke, t.ex. välbefinnandekohorten.
3. **Vad producenterna får tillbaka.** Ett forskningsdeltagande som är osynligt för användaren är inte långsiktigt hållbart. Minimi: personlig benchmarkrapport per säsong; namn i medförfattarförteckning på publikationer där deltagande varit substantiellt; förtur till nya plattformsfunktioner som forskningen lett till.
4. **Etisk granskning.** Externa studier som använder tier 2 eller tier 3 ska gå igenom ordinarie forskningsetisk prövning (Etikprövningsmyndigheten) innan dataåtkomst beviljas. Vi är inte etikprövningsnämnd — men vi kan inte vara en kanal förbi den heller.
5. **Teknisk isolering.** Forskningsuttag sker via ett separat read-replica, inte mot produktionsdatabasen. `domain_events`-strukturen gör det här mekaniskt enkelt.

Governancepaketet är också ett pitch-verktyg. En forskare som överväger att kalt-bygga ett dataset genom egna enkäter kommer alltid att välja en plattform där rättsmässigheten redan är löst.

---

## Ärliga begränsningar

Varje forskningstråd ovan står och faller med datasetets faktiska kvalitet vid studiens genomförandetidpunkt. Tre begränsningar som texten inte får låtsas bort:

- **Urvalsskevhet.** Tidiga användare av båda plattformarna är inte representativa för svensk snittblomsodling i stort — de är mer digitala, sannolikt yngre i medelålder, och geografiskt överrepresenterade i Skåne, Göteborgs- och Stockholmsregionerna. Under de första 2–3 åren begränsar det den externa validiteten i nästan alla kvantitativa skattningar. De bästa tidiga forskningsinsatserna är de som kan internt valideras eller som inte behöver generaliseras till hela populationen.
- **Opt-in-bias inom plattformen.** Även bland plattformsanvändare kommer forskningsdeltagande (särskilt tier 3) att vara självselekterat. Det påverkar särskilt välbefinnandetråden, där de odlare som mår sämst är minst benägna att svara på enkäter. Studiedesign behöver explicit hantera detta (weighting, sensitivity analysis, attrition bounds).
- **Tidsfördröjning.** Flera av de mest intressanta trådarna (fenologi, sortprovning, kohortvälbefinnande) kräver 3+ års historik för att bli meningsfulla. Forskningsplanen bör alltså tidssättas i faser: år 1–2 är metodutvecklings- och piloteringsåren, år 3+ är där publikationerna kommer.

---

## Coda: långhorisont-spekulation (5+ år)

Om plattformen når mognad och kan finansiera berikning utöver det som är realistiskt de närmaste åren, blir några ytterligare områden tillgängliga: pollinatorkameror per bädd kopplade till artidentifierings-ML; drönarfotogrammetri för biomassa och blomintensitet; kopplad kommunaldata (markanvändning, jordtyp från SGU) för platsrekommendationer; biobank-kopplad sortgenomik i samarbete med NordGen; och — det mest spekulativa — integration med svensk klimatforskningsinfrastruktur (ICOS-stationer, SITES) så att enskilda odlarbeddar blir flirtigt små men strukturellt jämförbara noder i ett nationellt klimatobservationsnät.

Inget av det här är något vi planerar för idag. Men att ha trådändarna synliga gör det lättare att säga ja när möjligheten en dag dyker upp.

---

*Dokumentet uppdateras i takt med att plattformen, dataseten och forskarkontakterna utvecklas. Trådar som övergår från visionär menu till pågående samarbete flyttas ut i egna uppföljningsdokument.*
