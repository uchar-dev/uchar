# jutterswet-initialisatie-per-dag — the initialisation on a timeline target, per day

An initialisation on an attribute for every month is the lowest layer of its target's stack, read per day
(`spec/RUNTIME-SEMANTICS.md` ch. 7.8): it decides every day on which no writer decides and the input reads no value,
over all time, open segments included (ch. 3.1). Its condition is read as the condition of a rule on a timeline target
(ch. 3.10). The cases follow the measurement of ALEF (an initialisation alone; beside a rule that writes part of the
year, in both orders; beside input; beside a rule that reads leeg on part of its period), in which ALEF's lazy routes
and its generated eager route fill every such day with the value of the initialisation. Own Jutterswet-flavoured
casus, own dates, rights-free.

## Casus (own text)

> **Art. 3h (strandloon per maand).** 1. Een jutter ontvangt voor elke maand een strandloon van 5, voor zover voor die
> maand geen ander bedrag geldt. 2. Het voorjaarsloon en het zomerloon bedragen in de maanden maart tot en met mei 2025
> 20. 3. Het kustloon en het havenloon zijn in de maanden maart tot en met augustus 2025 gelijk aan het basisloon van
> de jutter. 4. Het stormloon bedraagt in de maanden maart tot en met mei 2025 20 en in september en oktober 2025 30;
> in de overige maanden waarin de jutter stormwacht is, bedraagt het 5. 5. Een door de strandvoogd vastgesteld bedrag
> gaat voor het bedrag van lid 1.

## Source (`source.rgs`)

Every attribute is a whole number for every month; `stormwacht` is a characteristic for every month.

- `strandloon`: the initialisation on 5 alone.
- `voorjaarsloon`: 20 from 2025-03-01 up to 2025-06-01 and the initialisation on 5, the rule first.
- `zomerloon`: the same, the initialisation first.
- `wachtloon`: the initialisation on 5 alone, beside input in case 2.
- `kustloon`: the basisloon from 2025-03-01 up to 2025-09-01 and the initialisation on 5, the rule first.
- `havenloon`: the same, the initialisation first.
- `stormloon`: 20 in spring, 30 from 2025-09-01 up to 2025-11-01, and the initialisation on 5 "gedurende de tijd dat
  hij stormwacht is": two computing rules beside a conditional initialisation.

## Cases (hand-computed, absolute)

| Case | Input | Pinned |
|---|---|---|
| `case1-zonder-invoer` | basisloon 20 on [2025-03-01, 2025-06-01); stormwacht on 2025 | strandloon and wachtloon **5** on all time; voorjaarsloon, zomerloon, kustloon and havenloon **5** on (−∞, 2025-03-01), **20** on [2025-03-01, 2025-06-01), **5** on [2025-06-01, +∞), the kustloon and the havenloon also from June to August, where their rule reads leeg; stormloon **5**, **20**, **5**, **30**, **5** within 2025 and leeg outside it |
| `case2-invoer` | voorjaarsloon 10 on [2025-01-01, 2025-04-01); wachtloon 10 on [2025-03-01, 2025-06-01); stormloon 7 on [2024-01-01, 2025-02-01); stormwacht on 2025; no basisloon | voorjaarsloon **5** on (−∞, 2025-01-01), the input **10** on [2025-01-01, 2025-03-01), **20** in spring, **5** from June; wachtloon **5**, **10** in spring, **5**; kustloon and havenloon **5** on all time; stormloon the input **7** on [2024-01-01, 2025-02-01), **5** in February, **20**, **5**, **30**, **5** to the end of 2025 |
| `case3-zonder-stormwacht` | — | as case 1 without a basisloon; stormloon **20** in spring and **30** in September and October only: the condition of its initialisation holds on no day |

In case 2 the rule decides March, where the input 10 of the voorjaarsloon has a value too: a writer goes before the
input (ch. 7.8). The order of a rule and an initialisation in the document gives no difference (voorjaarsloon and
zomerloon, kustloon and havenloon): ALEF's eager interpreter gives leeg in spring where the initialisation stands
before the rule in an imperative rule group, an order artefact uchar has no place for.

ref and the ASP backend play every case: the backend reads the first six targets through the ch. 7.7 desugaring as a
defaults group per day (ch. 7.11), and the stormloon natively. The Drools backend refuses the golden on both branches
(DROOLS-118), and gate 12 lists it in its channel.
