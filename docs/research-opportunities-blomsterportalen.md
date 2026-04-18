# Forskningsmöjligheter — Blomsterportalen (enbart marknadsplatsdata)

*Intern visionstext. Variant av `research-opportunities.md` som begränsar sig strikt till Blomsterportalens data — det som säljs, av vem, till vem, när, till vilket pris och genom vilken logistikkedja. Ingen odlingsinformation, inga bäddar, inga sorthistorier från producenters odlingsapp. Syftet är att kartlägga vad som går att forska på redan när Blomsterportalen står på egna ben, innan eventuell integration med Verdant eller andra odlarorienterade system.*

---

## Varför Blomsterportalens data räcker långt

En väl instrumenterad marknadsplats är en ovanligt kraftfull forskningsinfrastruktur. Inte för att den ser allt — en handelsplats ser aldrig *varför* en blomma blev som den blev — utan för att den ser det ingen annan ser: den fullständiga transaktionen. Vem som sålde, vem som köpte, för vilket pris, vilka substitutioner som skedde, vilken logistikväg stjälken tog, när fakturan landade och om något klagades in. I den svenska snittblomsbranschen existerar inget motsvarande observationsfönster idag.

Blomsterportalen skiljer sig från generiska marknadsplatser i fyra avseenden som tillsammans gör datasetet forskningsintressant även utan odlingssidan: (i) **BankID-verifierad identitet**, vilket ger en juridisk och teknisk grund för longitudinella kohortstudier som inte är tillgänglig i anonyma plattformar; (ii) **event-sourcad arkitektur** (`domain_events`), som låter forskaren rekonstruera systemets tillstånd vid godtyckliga tidpunkter och göra kontrafaktuell analys; (iii) **transparent prismekanik** — deklarerade producentreservationspriser (`StandingPriceList`), öppen kommissionsstruktur, radnivå-uppdelning av faktura; och (iv) **en deterministisk substitutionsmotor** vars tier-fallback styrs av nätverkstillgänglighet snarare än av köparens val, vilket skapar kvasi-exogen variation som är svår att replikera i andra dataset.

Det här dokumentet kartlägger vad de egenskaperna öppnar för — inom klimat och miljö, landsbygd och försörjning, konsumentbeteende, kvalitet och distribution, samt marknadsdesign.

### Antaganden om data

Texten antar ett **2–4-årsfönster** där Blomsterportalens modell fått några års historia och kompletterats med två blygsamma tillägg: (1) strukturerad post-leverans-återkoppling från florister (realiserad vaslivslängd, kvalitetsbedömning på radnivå), och (2) opt-in konsumentkvitton när B2C-spåret öppnar. Inget av detta förutsätter integration med odlarplattformar.

---

## Strategisk motivering — varför vi vill möjliggöra det här

1. **Datanätverkseffekter.** Varje ny producent och florist stärker datasetets forskningsvärde parallellt med det kommersiella värdet. Ett explicit forskningsspår gör dynamiken synlig.
2. **Försvarbarhet.** Transaktionsdata är kopierbart. Ett longitudinellt, BankID-förankrat, event-sourcat och transparent dataset byggt över åren är det inte.
3. **Samfinansiering.** Formas, Vinnova, Horizon Europe och EIP-AGRI finansierar datadrivna plattformar av den här typen. Förberedda forskningsplaner förkortar ansökningscykeln avsevärt.
4. **Aktörslojalitet.** En producent eller florist som får benchmarkrapporter tillbaka, vars data bidrar till publicerad forskning, och som ser sitt namn som medförfattare är inbäddad i plattformen på ett kvalitativt annorlunda sätt.

---

## 1. Klimat och miljö

Snittblommor är klimatpolitiskt underskattade. Sverige importerar den absoluta majoriteten av sin konsumtion — främst från Nederländerna, sekundärt från Östafrika — vilket innebär växthus- och flygberoende försörjning av en produkt utan kalorisk eller strukturell funktion. Blomsterportalen ligger mitt i omställningen mot lokal produktion och har data som gör den omställningen mätbar.

### 1.1 Livscykelanalys för distributionsledet på radnivå

**Hypotes:** Den klimatmässiga skillnaden mellan olika distributionsvägar inom svensk snittblomshandel är mätbar, betydande och heterogen — och variationen förklaras av faktorer Blomsterportalen redan fångar: hub-topologi, antal leggar, kylkedjeklass, fordonsklass, och paketeringseffektivitet.

**Vad datan tillåter.** Traditionella LCA-studier av snittblommor (Williams 2007, Franze & Ciroth 2011) arbetar med generiska distributionsantaganden: en hypotetisk lastbil, ett hypotetiskt kylflöde, ett hypotetiskt sista-kilometer-beteende. Blomsterportalen har faktiska `Shipment`-leggar, faktiska `TemperatureTrace`-serier, faktiska `Manifest`-fyllnadsgrader, och kan koppla varje stjälk i en faktura till dess fysiska transportväg. Även utan odlingsfasens primärdata — som måste approximeras med generiska databaser eller schablonantaganden för den svenska populationen — kan distributionsfasens LCA göras med en granularitet som inte finns för någon annan färskvarusektor i landet.

**Vad det kan driva.** Resultaten utgör underlag både för operativt beslutsfattande (hur mycket klimatkostar en extra regional leverans kontra att konsolidera?) och för policyrelevanta frågeställningar (är kortare ledtider klimatmässigt försvarbara om de kräver mindre fordon?). Kandidatpartner: Stockholm Environment Institute, Chalmers (Division of Environmental Systems Analysis), IVL Svenska Miljöinstitutet, KTH Industrial Ecology. Metodhint: hybrid process-LCA med Monte Carlo-osäkerhet; känslighetsanalys för de produktionsfas-antaganden som måste göras utan primärdata.

### 1.2 Cirkulär ekonomi i logistiknätverket

**Hypotes:** Den samlade cirkulariteten i svensk snittblomsdistribution — från transportbehållare (CC-containrar, hinkpool) till emballage och sekundärförpackningar — är både mätbar och aktivt påverkbar via plattformsdesign, men har aldrig kvantifierats eftersom ingen aktör hittills haft vare sig överblick eller datastruktur för det.

**Datan som möjliggör mätningen.** Blomsterportalens `AssetPool`-modell kombinerat med radnivå-`ShipmentLeg` och `Manifest` ger fullständig spårbarhet för återanvändbara enheter: hur många rundor gör en genomsnittlig hink innan den tas ur bruk, var i nätverket förluster sker, hur fördelar sig skadefrekvensen mellan feeder-leggar och regionala leggar, och vad det kostar i ersättning. För första gången kan en svensk distributionssektor redovisa en nästan komplett Material Flow Analysis utan att någon aktör behöver rapportera manuellt.

**Varför det är politiskt aktuellt.** EU:s Ecodesign for Sustainable Products Regulation (ESPR) kommer successivt att kräva digitala produktpass för allt fler produktkategorier. Snittblommor ligger i praktiken i kön. Att ha en etablerad datainfrastruktur redo gör Blomsterportalen till naturlig referensimplementation — och det händelsebaserade eventlagret passar ovanligt väl som grund för produktpassens spårbarhetskrav. Kandidatpartner: IVL Svenska Miljöinstitutet, RISE, Chalmers miljösystemanalys. Metodhint: MFA med fallerings- och återanvändningsrater estimerade från `AssetPool`-eventhistorik; jämförelse mot konventionella engångsflöden.

### 1.3 Klimatmärkningarnas empiriska prispremie och köpbeteende

**Hypotes:** Klimat- och ursprungsmärkningar (`KRAV`, `Svenskodlat`) genererar både en mätbar prispremie och ett annorlunda köpmönster — men premien är sannolikt ojämn över köparsegment, säsong och produktkategori på sätt som befintliga konsumentenkäter inte kan fånga.

**Varför BP-data är rätt lins.** Forskning om betalningsvilja för hållbarhetsmärkning har i huvudsak byggt på hypotetiska köpbeslut i enkäter eller experiment, med välkänd hypotetisk-bias-problematik. Blomsterportalens orderdata visar *verkliga* köp, med transparent prissättning, och med florist-köpare vars affärsidentitet är känd (vilket öppnar för segmenterad analys: lyxflorister, livsmedelsbutikernas blomavdelningar, eventflorister). Premien kan estimeras med hedonic regression där produktegenskaper hålls konstanta, och beteendemönster kan följas longitudinellt per köpare.

**Varför det är intressant.** Om märkningar *faktiskt* driver betalningsvilja i kommersiella kanaler skiljer sig implikationerna radikalt mot om det bara är enkätartefakter. Politiskt viktigt — EU:s greenwashing-direktiv och svensk konsumentlag kräver alltmer empirisk grund för klimatmärkningars värdeförankring. Kandidatpartner: Handelshögskolan i Stockholm (House of Innovation), Centrum för konsumtionsforskning vid Göteborgs universitet, AgriFood Economics Centre vid SLU/Lund. Metodhint: hedonic regression, diff-in-diff vid certifieringsövergångar, panel med florist-fixed effects.

---

## 2. Människor och försörjning

Blomsterportalen är, utan överdrift, den första platsen där svensk småskalig snittblomssektor blir systematiskt observerbar. Bolagsverket-registrering, BankID-verifierad identitet, verifierade `Payout`-serier, och rollstruktur per `Organization` ger en vy som jordbruksstatistiken aldrig kommer att ge — eftersom sektorn per definition faller under tröskelvärdena.

### 2.1 Landsbygdsutveckling och den nya småbruksekonomin

**Hypotes:** Småskalig snittblomsodling — ett segment som knappast existerade i Sverige före 2015 — har vuxit snabbast i kommuner med kombinationen (a) pendlingsavstånd till tätort, (b) nedläggning av traditionell animalieproduktion, och (c) lokal florist- och marknadsinfrastruktur. Plattformen kan för första gången visa den geografiska fördelningen av denna "nya småbruksvåg" i realtid — utan att kräva odlingsdata.

**Datan och metoden.** Blomsterportalens `Organization`-data (Bolagsverket-registrering, KYB-validering via Roaring.io) kombinerat med `Payout`-serier ger en direkt mätning av var nyetablering sker, hur livslängden ser ut, och vilka försörjningsnivåer som faktiskt är uppnåbara. Eventhistoryanalys för verksamhetsöverlevnad och geografiskt viktade regressioner är lämpliga verktyg. Kopplingen till Jordbruksverkets strukturregister och SCB:s företagsregister — via organisationsnummer — gör det möjligt att placera sektorn i ett större rural-ekonomiskt sammanhang, trots att den idag faller under de flesta officiella tröskelvärdena.

**Varför det är policyrelevant.** Svensk landsbygdspolitik och CAP-implementering utgår fortfarande från en jordbruksmodell där snittblommor är osynliga. Samtidigt är detta en av få sektorer där nya jordbrukare — ofta unga, ofta kvinnor, ofta utan agronomisk familjebakgrund — etablerar sig. Empirisk lucka med direkta politiska implikationer. Kandidatpartner: SLU institutionen för stad och land, SLU Future Food, Jordbruksverkets analysenhet, Tillväxtanalys.

### 2.2 Odlares välbefinnande via BankID-kohort (BP-indikatorer)

**Hypotes:** Odlares subjektiva välbefinnande över tid är starkare kopplat till *förutsägbarheten* i efterfrågan (cykelstabilitet, substitutionsfrekvens, sena avbeställningar, klagomålsfrekvens) än till nettopayout-nivån. Hypotesen är testbar även utan odlingsdata — Blomsterportalens händelseregistrering ger tillräckligt rika indikatorer på arbetsbelastning och marknadsrelationell stress.

**Den metodologiska möjligheten.** BankID-autentiseringen är den tekniska och juridiska grunden. Genom opt-in kan en producentkohort kopplas där plattformsindikatorer — listningsfrekvens, orderhanteringstempo (implicerat söndagsarbete i cykelns upplägg), `Payout`-variabilitet, `ReliabilityScore`-historik, `Claim`- och substitutionsexponering — matchas mot validerade välbefinnandeinstrument (WHO-5, UWES, COPSOQ III) administrerade i återkommande vågor. Motsvarande koppling är idag i princip omöjlig utanför registerforskning, och registerforskning kan inte observera plattformsbeteende. Kombinationen är unik också utan odlingssidans arbetsbelastningsdata.

**Varför det spelar roll för produkten.** Om hypotesen stämmer blir argumentet för att investera i cykelstabilitet (reducera sena avbeställningar, förbättra substitutionsprediktion, sänka klagomålsfrekvens) inte ett UX-argument utan ett folkhälsoargument — vilket öppnar finansiering som annars är stängd. Kandidatpartner: Karolinska Institutets IMM, Stressforskningsinstitutet vid Stockholms universitet, Gothenburg Research Institute. Metodhint: longitudinella latenta tillväxtkurvor; inom-person fixed effects för att identifiera förutsägbarhetseffekter separat från nivåeffekter.

### 2.3 Genus och vem som säljer blommor

**Hypotes:** Svensk småskalig snittblomssektor är, helt i motsats till traditionellt jordbruk, statistiskt kvinnodominerad — både bland producenter och bland florister. Den dominansen är inte slumpmässig utan strukturerad av ingångskanaler, arbetsfördelning inom organisationer och hur generationsväxling skiljer sig från familjelantbruksmodellen.

**Datan.** `Membership.role`-strukturen i Blomsterportalen tillsammans med användarmetadata (BankID ger verifierat kön och ålder där samtycke finns) gör det möjligt att kartlägga rollfördelning per kön, ålder, organisationsstorlek och region över tid. Särskilt intressant är dynamiken inom flerpersonsorganisationer: vem är registrerad som `owner`, vem som `listing_manager`, vem som `packer` — och hur förändras den fördelningen när ett par driver verksamheten tillsammans? Samma analys kan göras på florist-sidan (`owner`, `buyer`, `shop_staff`).

**Varför det är viktigt.** Den maskulina dominansen i svenskt jordbruk har varit föremål för betydande forskning, men snittblomssektorn som motexempel — både på produktions- och återförsäljarsidan — har knappast studerats. Kandidatpartner: Göteborgs universitet (genusvetenskap), SLU institutionen för stad och land, Uppsala universitet (Centre for Gender Research), NORA-nätverket. Metodhint: komparativ kvantitativ analys mot LRF:s medlemsdata och Jordbruksverkets strukturregister, kompletterad med djupintervjuer.

### 2.4 Floristens ekonomi och B2B-köparens livscykel

**Hypotes:** Florist­verksamhet är en av Sveriges tystaste småföretagssektorer — geografiskt spridd, ofta ensamt driven, och med låg offentlig datatillgänglighet. Blomsterportalen får för första gången en full observation av florist-sidan av flödet: köpmönster över cykler, inköpsportföljens variation, svarsmönster på substitutioner och klagomål, samt överlevnadsdynamik.

**Datan.** Florist-`Organization` med KYB-validering, `Cart` och `Order`-historik per cykel, betalningsdata (inbetalningsdisciplin, ev. `FactoringRequest`), tvistehistorik (`Claim`, `Resolution`), samt — kritiskt — den långsiktiga sekvensen av aktivitet → inaktivitet → återkomst → avslutning. Att observera B2B-köpares livscykel med den här upplösningen saknar motsvarighet i svensk kommersiell forskning.

**Frågor.** Hur ser överlevnadskurvan ut för en nystartad florist? Finns systematiska mönster i vilka köpare som blir långlivade — är det bredd i inköpsportfölj, disciplin i betalning, låg klagomålsfrekvens, eller något annat? Hur reagerar florister på plattformsinterventioner (nya funktioner, ändrade avgifter, substitutionspolicy-uppdateringar)? Kandidatpartner: Handelshögskolan i Stockholm (Center for Retailing), Entreprenörsskapsforum, Jönköping International Business School (entreprenörskap och småföretag). Metodhint: survival analysis; sekventiell mönsterutvinning; panel-regressioner med plattformsfunktioner som behandlingar.

---

## 3. Konsument och välbefinnande

### 3.1 Biophilia, gåvokultur och affektiv konsumtion

**Hypotes:** Blomköp är ett av de mest affektivt laddade och samtidigt minst empiriskt studerade konsumtionsmönstren i svensk handel. Köpbeslutet drivs av en komplex blandning av humörreglering, social signalering och estetisk preferens — en blandning som i existerande litteratur rekonstruerats via enkäter och laboratorieexperiment snarare än observerad transaktionsdata. Vid öppnandet av B2C-spåret kommer Blomsterportalen att ha ett av Europas tätaste observationsfönster mot dessa mekanismer.

**Vad datan tillåter.** Med samtyckesgraderade konsumentkvitton kan man koppla *vem* som köper *vad* åt *vem* (gåva vs. egen konsumtion, via leveransadressmatchning) *när* (säsong, vardagar vs. helger, närhet till livshändelser) till produktegenskaper som ursprung, certifiering och pris. Frågor: I vilken utsträckning drivs blomköp av humörreglering kontra social signalering? Hur skiljer sig gåvomönster mellan åldrar, livshändelser och geografiska kontexter? Vilken roll spelar svenskhetsmärkning för upplevt värde och återköpsfrekvens?

**Identifikation och partners.** Kausal identifikation av emotionell elasticitet kräver exogena chocker; leveransförseningar orsakade av väder eller logistikstörningar fungerar som instrument. Kandidatpartner: Handelshögskolan i Stockholm (House of Innovation, Center for Retailing), Centrum för konsumtionsforskning vid Göteborgs universitet, Karolinska Institutet (affektiv psykologi). Metodhint: instrumentvariabel-approach, segmenterad diff-in-diff för märkningseffekter, hierarkisk modellering för hushållsnivå-variation.

### 3.2 Sorg- och minneskonomi

**Hypotes:** Blommor vid begravning, årsdagar, jubileer och andra minnesögonblick utgör en ekonomiskt betydande men kvantitativt nästan osynlig del av svensk snittblomskonsumtion. Denna "minneskonomi" har emotionell och kulturell laddning som gör den svår att studera med konventionella metoder — respondenter är obekväma, konsumenter minns felaktigt, och transaktionsdata saknas.

**Datan.** Till skillnad från bröllops- eller eventmarknaden — där stora belopp ändå hanteras relativt synligt — är sorgekonomin fragmenterad: enskilda konsumenter, små florister, oregelbundna transaktioner, ofta korta ledtider. Blomsterportalens orderdata (sort, mängd, pris, leveransdatum, mottagaradress) kombinerat med florist-kategorisering och säsongsvariation ger för första gången en strukturerad observationsram. Longitudinell koppling till konsumenter som återkommer årsvis vid samma datum är metodologiskt kraftfull.

**Varför det är värdefullt.** Forskning om sorg, minne och konsumtion är en aktiv skärningspunkt mellan konsumentsociologi, death studies och kulturekonomi, men systematisk data saknas. Kandidatpartner: Centrum för konsumtionsforskning vid Göteborgs universitet, Uppsala universitets centrum för forskning om religion och samhälle, Lund (kulturvetenskap). Etiskt känsligt — särskild varsamhet kring samtycke krävs. Metodhint: survival analysis för återbesöksintervall; kvalitativ komplettering via djupintervjuer med samtyckande konsumenter.

---

## 4. Kvalitet och distribution

### 4.1 Post-harvest: från listningskvalitet till realiserad vaslivslängd

**Hypotes:** Det finns en systematisk och mätbar gap mellan deklarerad vaslivslängd på produktlistningar och faktisk vaslivslängd hos slutkonsument — och den gapens storlek är en funktion av mätbara faktorer: produktens kvalitetsgrad vid listning, skördestadium (där det rapporteras), kylkedjetemperaturprofil, antal leggar i transportkedjan, och tid-till-slutkund.

**Datan.** Post-harvest-forskning har länge identifierat kylkedjebrott som huvudfaktor i vaslivslängdsreduktion, men har saknat storskaliga dataset som kan kvantifiera effekten under verkliga distributionsförhållanden. Blomsterportalens produktlistning (med `grade`, deklarerad vaslivslängd, stjälklängd), `TemperatureTrace` per `ShipmentLeg`, och strukturerad florist-återkoppling på realiserad vaslivslängd (förutsätter ett tillägg som ligger naturligt i produktens utveckling) ger ett komplett distributionskedjedataset — även om den allra första processnoden (faktiska odlingsförhållanden) förblir utanför modellen.

**Vad det möjliggör.** En kalibrerad modell för *förväntad* vaslivslängd per produkt-till-kund-väg skulle låta Blomsterportalen sätta realistiska löften och upptäcka kylkedjeproblem i realtid. För forskningen öppnar det dos-respons-kurvor för temperaturavvikelser, interaktion mellan kvalitetsgrad och kedjestress, och validering av etylenskyddsstrategier i verklig distribution. Kandidatpartner: SLU Alnarp post-harvest physiology, Aarhus University Department of Food Science. Metodhint: överlevnadsanalys med konkurrerande risker; kylkedjebrott som tidsberoende kovariat; strukturella ekvationsmodeller för att separera produktlatenta egenskaper från kedjeeffekter.

### 4.2 Kylkedjan som observerbart system

**Hypotes:** Snittblommors kylkedja är, till skillnad från livsmedelskylkedjor, varken reglerad eller välstuderad — trots att känsligheten är jämförbar. Blomsterportalens `TemperatureTrace` per leg ger för första gången ett rikt longitudinellt dataset för att studera kylkedjans faktiska prestanda i ett nordiskt distributionsnätverk.

**Frågor som blir besvarbara.** Var i nätverket uppstår flest temperaturavvikelser? Är de koncentrerade till vissa rutter, fordonstyper, säsonger, eller operatörer? Hur ofta utlöser avvikelserna faktiska `ShipmentIncident`-händelser, och hur stort är det "tysta spillet" av avvikelser som aldrig resulterar i dokumenterade incidenter? Hur korrelerar kylkedjeprestanda med utomhustemperatur och vädertyp — en klimatanpassningsfråga som blir mer angelägen när sommartemperaturer stiger?

**Varför det är publicerbart.** Forskning om kylkedjor är etablerad (t.ex. vid SLU, Aarhus University, och Wageningen) men observationsdata från verkliga distributionsflöden i Norden är förvånansvärt sparsam. Kandidatpartner: SLU, RISE (livsmedel och biovetenskap), Aarhus Universitet. Metodhint: anomalidetektering, tidsserieanalys, nätverkstopologisk analys av var i kedjan avvikelser koncentreras.

---

## 5. Marknader och system

### 5.1 Substitutionskaskaden som naturligt experiment

**Hypotes:** När Blomsterportalens substitutionsmotor ersätter en beställd produkt med en annan enligt förkonfigurerad floristpolicy, utgör utfallet ett kvasi-randomiserat behandlingsfenomen som kan användas för att identifiera kausal efterfrågerespons — något som är mycket svårt att få i marknadsplatsdata där substitutioner normalt är endogena.

**Varför det är ovanligt.** Kausalekonomisk identifikation av substitutionselasticiteter bygger oftast på svagt exogena instrument (prischocker från tredje part, skattechocker, säsongsskift). Blomsterportalens sexstegsfallback — där tier-tilldelningen styrs av tillgängligheten hos andra producenter i nätverket, inte av floristens eget val — ger något mycket närmare riktig exogenitet. Varje event lagras i `domain_events` med komplett attribution (producent, produkt, tier, pris, distansökning), vilket gör det möjligt att rekonstruera kontrafaktuella utfall med acceptabel statistisk kraft redan efter några tusen substitutioner.

**Publicerbarhet.** Plattformsekonomi som disciplin har i flera år letat efter dataset där substitutionsmekanik är (a) transparent, (b) kvasi-randomiserad, och (c) kopplad till verkligt konsumentutfall. Blomsterportalen kan leverera alla tre. Kandidatpartner: Handelshögskolan i Stockholm, IFN (Institutet för näringslivsforskning), Toulouse School of Economics, Göteborgs universitet (industriell organisation). Metodhint: regression discontinuity vid tier-tröskelvärden; instrument baserade på nätverkstillgänglighet snarare än pris.

### 5.2 Försörjningskedjans resiliens och helgdagschocker

**Hypotes:** Snittblomssektorn uppvisar några av kalenderårets mest extrema och förutsägbara efterfrågechocker — alla hjärtans dag, mors dag, midsommar, allhelgona, första advent, begravningssäsong. Dessa återkommande chocker, kombinerade med oförutsägbara (extremväder, hub-bortfall, pandemistörningar), gör Blomsterportalens nätverk till ett sällsynt lämpligt testobjekt för försörjningskedjeresiliens.

**Datan.** Trenivås-topologin (`HubGraph`: leaf → local → regional) tillsammans med `ShipmentIncident`-registret, `TemperatureTrace`-serier, `ReliabilityScore` per producent och rutt, och `CycleCalendar` med `HolidayProfile`-justeringar ger fullständig observabilitet av hur chocker propagerar. Varje helgdagstopp är i praktiken ett planerat stresstest; varje oväntad störning är ett naturligt experiment. Den händelsesourcade arkitekturen gör det möjligt att rekonstruera systemets tillstånd vid godtyckliga tidpunkter.

**Varför det är relevant bortom blommor.** Forskning om resiliens i livsmedels- och färskvarukedjor har länge begränsats av att aktörerna är stora, politiskt känsliga och svårobserverbara. En snittblomsplattform delar centrala egenskaper med lokala livsmedelskedjor (färskvara, kort ledtid, hub-och-spoke-topologi, säsongsvariation) men är avsevärt mer forskningsvänlig. Kandidatpartner: FOI, MSB:s forskningsråd, RISE kritisk infrastruktur, KTH Industrial Engineering and Management. Metodhint: nätverksanalys med cascadingfel-simulering; empirisk validering mot observerade incidenter; jämförande analys mellan planerade (helgdag) och oplanerade chocker.

### 5.3 Prissättning och marknadsdesign vid tunn likviditet

**Hypotes:** Blomsterportalens tvåveckorscykel med fasta cutoff-tider, `StandingPriceList` som producent-satt referens, RFQ-spår parallellt, och transparent avgiftsstruktur utgör en välavgränsad testbädd för klassiska frågor inom marknadsdesign som normalt är svåra att studera empiriskt: prisupptäckt vid tunn likviditet, reservationsvärden hos småskaliga säljare, och välfärdsimplikationer av olika allokeringsmekanismer.

**Unika datafördelar.** Standing price lists låter oss observera säljares deklarerade reservationspriser *innan* transaktion, vilket är ovanligt — i de flesta marknadsplatser är priserna dynamiska och endogena. Kombinationen med substitutionsmotorns tvingade matchningar ger en andra källa till exogen prisvariation. Tvåveckorscykelns fasta tempo (söndag 24:00 → tisdag, onsdag 24:00 → fredag) skapar en regelbunden struktur som förenklar paneldataanalys på sätt som ad hoc-marknadsplatser inte gör. Regional hub-topologi möjliggör dessutom analys av prisdispersion och law-of-one-price-avvikelser — samma `MasterProduct` kan kosta olika i Malmö, Göteborg och Stockholm, och variationen över tid är en direkt observation av marknadens integrationsgrad.

**Frågor och partners.** Hur reagerar småskaliga producenter på tunn likviditet? I vilken grad skiljer sig RFQ-spårets allokeringseffektivitet från cykelspårets? Ger transparenta kommissionsnivåer andra beteenden än dolda marginaler? Kandidatpartner: Handelshögskolan i Stockholm (marknadsdesigngrupp), IFN, Jönköping International Business School, Toulouse School of Economics. Metodhint: strukturell estimering av bud- och reservationsvärdesfördelningar; välfärdsanalys via simulering av alternativa mekanismer; jämförande analys mot historiska svenska jordbruksauktioner.

### 5.4 RFQ-spåret som sealed-bid-auktion

**Hypotes:** RFQ-spårets sealed-bid-struktur, där producenter lägger bud inom en fönstertid och florister tilldelar — möjligen till flera — utgör en av de få implementerade, verklighetsförankrade sealed-bid-marknaderna i svensk färskvarusektor. Det öppnar för empirisk prövning av teoretiska resultat i auktionslitteraturen som sällan kan testas utanför laboratoriemiljö.

**Vad som är forskningsvärt.** Sealed-bid-auktioner i ett leverantörsnätverk med multi-winner-tilldelning är teoretiskt rika: budkolusion vs. konkurrens, optimala budstrategier vid lead-time-stratifierade depositioner, risker för winner's curse, och fördelningseffekter när små producenter konkurrerar mot stora. Blomsterportalens `RFQ`- och bud-data — inklusive bud som inte vinner, vilket i sig är värdefull information som sällan finns tillgänglig för forskare — ger en mycket ren observationsgrund.

**Partners och metodik.** Kandidatpartner: Handelshögskolan i Stockholm (marknadsdesigngrupp), IFN, Toulouse School of Economics (där auktionsteori bedrivs internationellt ledande). Metodhint: strukturell estimering av bud-funktionsform; jämförelse av observerade bud med teoretiskt optimala under olika antaganden om konkurrens; välfärdsimplikationer av alternativa depositionsregler.

---

## Forskningsgovernance — hur vi gör det här utan att förlora vår själ

Att möjliggöra forskning utan en tydlig governancestruktur är ett sätt att bränna producenternas och floristernas förtroende. Minimikraven:

1. **Samtyckesmodell.** BankID-autentiseringen ger den juridiska grunden att fråga individer om utvidgat samtycke för forskningsanvändning. Samtycke ska vara granulärt (per forskningsområde eller studie, inte ett enda klumpsamtycke), återkallbart, och loggat.
2. **Datatiers.**
   - **Tier 1 — aggregerat, öppet:** benchmarkdata, branschrapporter, årsöversikter. Ingen radnivå.
   - **Tier 2 — radnivå, akademisk, under DUA:** pseudonymiserade dataset för ackrediterade forskare, bundet till data-use agreement, icke-kommersiellt.
   - **Tier 3 — samtyckesgradad identitet:** endast för specifika studier med explicit individuellt samtycke.
3. **Vad deltagarna får tillbaka.** Personliga benchmarkrapporter; namn i medförfattarförteckning på publikationer där deltagande varit substantiellt; förtur till nya plattformsfunktioner som forskningen lett till.
4. **Etisk granskning.** Externa studier som använder tier 2 eller tier 3 ska gå igenom ordinarie forskningsetisk prövning (Etikprövningsmyndigheten) innan dataåtkomst beviljas.
5. **Teknisk isolering.** Forskningsuttag sker via ett separat read-replica, inte mot produktionsdatabasen. `domain_events`-strukturen gör det här mekaniskt enkelt.

---

## Ärliga begränsningar

Varje forskningstråd ovan står och faller med datasetets faktiska kvalitet vid studiens genomförandetidpunkt. Tre begränsningar som texten inte får låtsas bort:

- **Produktionssidan förblir en svart låda.** Utan odlingsdata saknar datasetet biologiska orsaksvariabler. Flera trådar (särskilt LCA och post-harvest) skulle vara avsevärt starkare med en Verdant-integration; utan den måste vi approximera produktionsfasen med generiska LCA-databaser och sektor-genomsnitt.
- **Urvalsskevhet.** Tidiga användare av Blomsterportalen är inte representativa för svensk snittblomssektor i stort — mer digitala, sannolikt yngre i medelålder, geografiskt överrepresenterade i storstadsregionerna. Under de första 2–3 åren begränsar det den externa validiteten i nästan all kvantitativ skattning.
- **Tidsfördröjning.** Flera av de mest intressanta trådarna (välbefinnandekohort, florist-livscykel, holiday-shock-serier) kräver 3+ års historik för att bli meningsfulla. Forskningsplanen bör tidssättas i faser: år 1–2 är metodutvecklings- och piloteringsåren, år 3+ är där publikationerna kommer.

---

## Coda: vad som öppnas med en Verdant-integration eller andra tillägg

Om Blomsterportalens dataset en dag knyts samman med Verdant eller motsvarande odlarorienterad plattform öppnas flera trådar som är utom räckhåll idag: fenologisk forskning på odlade prydnadsgrödor, pollinatör-blomma-interaktioner på bäddnivå, federerad sortprovning med kommersiella utfall inbäddade, och LCA där odlingsfasens primärdata är observerad snarare än approximerad. Längre ut finns pollinatörkameror, drönarbilder, biobank-kopplad sortgenomik och integration med svensk klimatforskningsinfrastruktur (ICOS, SITES). Inget av det här är något vi planerar för idag, men att ha trådändarna synliga gör det lättare att säga ja när möjligheten en dag dyker upp.

---

*Dokumentet uppdateras i takt med att plattformen, datasetet och forskarkontakterna utvecklas. Trådar som övergår från visionär meny till pågående samarbete flyttas ut i egna uppföljningsdokument.*
