package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RUNTIME-SEMANTICS ch. 6.1 / ch. 3.4a / ch. 3.4d — a NEGATED characteristic read hidden inside a fold's
 * die/dat filter, or inside a §5.7 union selection's filter, is a STRATIFICATION edge like any other.
 *
 * <p><b>What this catches, measured 2026-09-22 before the fix.</b> {@code Stratificatie.verzamelReads}
 * descended into nested CONDITIONS only — a compound's bullets, a quantifier fold's filter and matrix — and
 * never into a condition's EXPRESSION operands. So a rule whose {@code voorwaarde} compares an aggregation
 * against a bound, where the aggregation's filter reads a DERIVED characteristic negatively, looked to the
 * analysis like a rule that reads nothing at all. With the consumer written ABOVE its producer, both Drools
 * branches fired it in the transient window — the element was not yet marked, so the count of unmarked
 * elements was positive — and a monotone characteristic grant is never taken back: {@code verwacht afwezig,
 * werkelijk aanwezig}, on a verifier-clean document that ref and the ASP backend both answer correctly,
 * because their dependency walk reaches a fold filter (the verifier's {@code dep_cond}, negation included).
 *
 * <p><b>Two halves, deliberately.</b> The gap was NOT the union's: the ORDINARY single-source fold with the
 * same filter diverged identically, and had done so since that filter landed. Both rows are here so that the
 * one walk which closes them is pinned from both sides — and so that a future reader is not told the union
 * invented a hole it only joined. The corpus golden {@code jutterswet-uniestratum} keeps the union half under
 * gates 5, 6 and 12 as well; the ordinary-fold half has no golden and lives here alone.
 *
 * <p>Both documents put the CONSUMER first in rule order, which is what makes the analysis load-bearing: in
 * producer-first order both branches answered correctly before the fix as well.
 */
class VouwfilterStratumWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_negatief_takfilter_van_een_unie_zinkt_de_regel(@TempDir Path dir) throws Exception {
        Path ir = dir.resolve("unie.ir.json");
        Files.writeString(ir, UNIE);
        BeideTakken.groen(ir, JSON.readTree(GEVAL),
                "de consument mag niet vuren voor haar producent, ook niet via een unie-takfilter");
    }

    @Test void een_negatief_vouwfilter_zinkt_de_regel_net_zo(@TempDir Path dir) throws Exception {
        Path ir = dir.resolve("vouw.ir.json");
        Files.writeString(ir, GEWONE_VOUW);
        BeideTakken.groen(ir, JSON.readTree(GEVAL),
                "dezelfde wet voor de gewone eenbronsvouw — het gat was nooit het gat van de unie");
    }

    private static final String UNIE = """
{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType",
        "name": "Jutter",
        "animate": true,
        "plural": "Jutters",
        "attributes": [],
        "characteristics": [
          {
            "node": "Characteristic",
            "name": "alarm",
            "kind": "adjectival"
          }
        ]
      },
      {
        "node": "ObjectType",
        "name": "Vondst",
        "animate": false,
        "plural": "Vondsten",
        "attributes": [
          {
            "node": "Attribute",
            "name": "gewicht",
            "datatype": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified"
            }
          }
        ],
        "characteristics": [
          {
            "node": "Characteristic",
            "name": "gemerkt",
            "kind": "adjectival"
          }
        ]
      }
    ],
    "factTypes": [
      {
        "node": "FactType",
        "name": "vondstaanmelding",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "aanmelder",
            "objectType": "Jutter",

            "many": false
          },
          {
            "node": "Role",
            "name": "aangemelde vondst",
            "objectType": "Vondst",
            "plural": "aangemelde vondsten",
            "many": true
          }
        ]
      },
      {
        "node": "FactType",
        "name": "vondstberging",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "berger",
            "objectType": "Jutter",

            "many": false
          },
          {
            "node": "Role",
            "name": "geborgen vondst",
            "objectType": "Vondst",
            "plural": "geborgen vondsten",
            "many": true
          }
        ]
      }
    ],
    "parameters": [],
    "domains": [],
    "unitSystems": [],
    "dimensions": [],
    "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "alarm bij ongemerkte vondsten", "versions": [ {
          "validity": {
          "node": "Validity"
          
        },
          "target": {
          "node": "AttributeReference",
          "attribute": "alarm",
          "subjectType": "Jutter",
          "source": { "snippet": "Een Jutter is alarm" }
        },
          "value": { "node": "Literal", "value": { "boolean": true }, "type": {
            "node": "Boolean"
          }, "source": { "snippet": "waar" } },
          "condition": {
          "node": "Comparison",
          "op": "greater",
          "left": { "node": "Aggregation", "kind": "count", "arguments": [
              {
                "node": "InstanceSetUnion",
                "branches": [
                  {
                    "node": "InstanceSetBranch",
                    "elementType": "Vondst",
                    "sourceRole": {
                      "node": "RoleBinding",
                      "factType": "vondstaanmelding",
                      "role": "aangemelde vondst"
                    },
                    "filter": {
                      "node": "CharacteristicCheck",
                      "characteristic": "gemerkt",
                      "negated": true,

                      "source": { "snippet": "niet gemerkt zijn" }
                    },
                    "source": { "snippet": "aangemelde vondsten van de Jutter die niet gemerkt zijn" }
                  },
                  {
                    "node": "InstanceSetBranch",
                    "elementType": "Vondst",
                    "sourceRole": {
                      "node": "RoleBinding",
                      "factType": "vondstberging",
                      "role": "geborgen vondst"
                    },
                    "filter": {
                      "node": "CharacteristicCheck",
                      "characteristic": "gemerkt",
                      "negated": true,

                      "source": { "snippet": "niet gemerkt zijn" }
                    },
                    "source": { "snippet": "geborgen vondsten van de Jutter die niet gemerkt zijn" }
                  }
                ],
                "source": { "snippet": "de unie van ongemerkte vondsten" }
              }
            ], "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "nonNegative"
            }, "source": { "snippet": "het aantal ongemerkte vondsten" } },
          "right": { "node": "Literal", "value": { "number": "0" }, "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified"
            }, "source": { "snippet": "0" } },

          "source": { "snippet": "het aantal ongemerkte vondsten groter is dan 0" }
        },
          "initialisation": false } ] },
      { "node": "Rule", "name": "gemerkt bij gewicht", "versions": [ {
          "validity": {
          "node": "Validity"
          
        },
          "target": {
          "node": "AttributeReference",
          "attribute": "gemerkt",
          "subjectType": "Vondst",
          "source": { "snippet": "Een Vondst is gemerkt" }
        },
          "value": { "node": "Literal", "value": { "boolean": true }, "type": {
            "node": "Boolean"
          }, "source": { "snippet": "waar" } },
          "condition": {
          "node": "Comparison",
          "op": "greater",
          "left": {
            "node": "AttributeReference",
            "attribute": "gewicht",
            "subjectType": "Vondst",
            "source": { "snippet": "het gewicht van de Vondst" }
          },
          "right": { "node": "Literal", "value": { "number": "5" }, "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified"
            }, "source": { "snippet": "5" } },

          "source": { "snippet": "het gewicht van de Vondst groter is dan 5" }
        },
          "initialisation": false } ] }
    ],
    "dayKindDefinitions": [],
    "objectCreations": [],
    "allocations": [],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [],
    "ruleGroups": []
  },
  "diagnostics": []
}
""";

    private static final String GEWONE_VOUW = """
{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType",
        "name": "Jutter",
        "animate": true,
        "plural": "Jutters",
        "attributes": [],
        "characteristics": [
          {
            "node": "Characteristic",
            "name": "alarm",
            "kind": "adjectival"
          }
        ]
      },
      {
        "node": "ObjectType",
        "name": "Vondst",
        "animate": false,
        "plural": "Vondsten",
        "attributes": [
          {
            "node": "Attribute",
            "name": "gewicht",
            "datatype": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified"
            }
          }
        ],
        "characteristics": [
          {
            "node": "Characteristic",
            "name": "gemerkt",
            "kind": "adjectival"
          }
        ]
      }
    ],
    "factTypes": [
      {
        "node": "FactType",
        "name": "vondstaanmelding",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "aanmelder",
            "objectType": "Jutter",

            "many": false
          },
          {
            "node": "Role",
            "name": "aangemelde vondst",
            "objectType": "Vondst",
            "plural": "aangemelde vondsten",
            "many": true
          }
        ]
      },
      {
        "node": "FactType",
        "name": "vondstberging",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "berger",
            "objectType": "Jutter",

            "many": false
          },
          {
            "node": "Role",
            "name": "geborgen vondst",
            "objectType": "Vondst",
            "plural": "geborgen vondsten",
            "many": true
          }
        ]
      }
    ],
    "parameters": [],
    "domains": [],
    "unitSystems": [],
    "dimensions": [],
    "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "alarm bij ongemerkte vondsten", "versions": [ {
          "validity": {
          "node": "Validity"
          
        },
          "target": {
          "node": "AttributeReference",
          "attribute": "alarm",
          "subjectType": "Jutter",
          "source": { "snippet": "Een Jutter is alarm" }
        },
          "value": { "node": "Literal", "value": { "boolean": true }, "type": {
            "node": "Boolean"
          }, "source": { "snippet": "waar" } },
          "condition": {
          "node": "Comparison",
          "op": "greater",
          "left": { "node": "Aggregation", "kind": "count", "arguments": [
              {
                "node": "AttributeReference",
                "attribute": "aangemelde vondsten",
                "subjectType": "Jutter",
                "source": { "snippet": "aangemelde vondsten van de Jutter" }
              }
            ], "elementType": "Vondst", "filter": {
              "node": "CharacteristicCheck",
              "characteristic": "gemerkt",
              "negated": true,

              "source": { "snippet": "niet gemerkt zijn" }
            }, "sourceRole": {
              "node": "RoleBinding",
              "factType": "vondstaanmelding",
              "role": "aangemelde vondst"
            }, "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "nonNegative"
            }, "source": { "snippet": "het aantal ongemerkte aangemelde vondsten" } },
          "right": { "node": "Literal", "value": { "number": "0" }, "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified"
            }, "source": { "snippet": "0" } },

          "source": { "snippet": "het aantal ongemerkte vondsten groter is dan 0" }
        },
          "initialisation": false } ] },
      { "node": "Rule", "name": "gemerkt bij gewicht", "versions": [ {
          "validity": {
          "node": "Validity"
          
        },
          "target": {
          "node": "AttributeReference",
          "attribute": "gemerkt",
          "subjectType": "Vondst",
          "source": { "snippet": "Een Vondst is gemerkt" }
        },
          "value": { "node": "Literal", "value": { "boolean": true }, "type": {
            "node": "Boolean"
          }, "source": { "snippet": "waar" } },
          "condition": {
          "node": "Comparison",
          "op": "greater",
          "left": {
            "node": "AttributeReference",
            "attribute": "gewicht",
            "subjectType": "Vondst",
            "source": { "snippet": "het gewicht van de Vondst" }
          },
          "right": { "node": "Literal", "value": { "number": "5" }, "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified"
            }, "source": { "snippet": "5" } },

          "source": { "snippet": "het gewicht van de Vondst groter is dan 5" }
        },
          "initialisation": false } ] }
    ],
    "dayKindDefinitions": [],
    "objectCreations": [],
    "allocations": [],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [],
    "ruleGroups": []
  },
  "diagnostics": []
}
""";

    private static final String GEVAL = """
{
  "format": "uchar-case-v0",
  "asOfDate": null,
  "input": {
    "instances": [
      {
        "label": "J1",
        "objectType": "Jutter",
        "attributes": {},
        "roles": {
          "aangemelde vondsten": [
            "V1"
          ]
        },
        "characteristics": []
      },
      {
        "label": "V1",
        "objectType": "Vondst",
        "attributes": {
          "gewicht": {
            "number": "10"
          }
        },
        "roles": {},
        "characteristics": []
      }
    ],
    "parameters": {}
  },
  "expected": [
    {
      "kind": "characteristicAbsent",
      "label": "J1",
      "characteristic": "alarm"
    }
  ]
}
""";
}
