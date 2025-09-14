/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This program and the accompanying materials are made
 * available under the terms of the Creative Commons 4.0
 * which is available at https://creativecommons.org/2014/01/07/plaintext-versions-of-creative-commons-4-0-licenses/
 *
 * SPDX-License-Identifier: CC-BY-4.0
 *
 * This product depends on software developed by the Open Source Automation Development Lab eG (OSADL) (https://www.osadl.org/).
 */

package org.eclipse.sw360.licenses.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.json.JSONObject;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.eclipse.sw360.datahandler.common.CommonUtils.TMP_OBLIGATION_ID_PREFIX;

public class OSADLObligationConnector extends ObligationConnector {
	public static final String EXTERNAL_ID_OSADL = "OSADL-Obligation-License";
	private static final Logger log = LogManager.getLogger(OSADLObligationConnector.class);
    private static final String SOURCE = "OSADL";
	private static final String BASE_URL = "https://www.osadl.org/fileadmin/checklists/unreflicenses/";
    private static final String CHECKLISTS = "https://www.osadl.org/fileadmin/checklists/all/unreflicenses.txt";
    private static final String OSADL_FORMAT = ".txt";

	@Override
    protected String generateURL(String licenseId) {
        return BASE_URL + licenseId + OSADL_FORMAT;
    }

    public static Mono<Obligation> get(String licenseId, User user) {
		OSADLObligationConnector osadlConnector = new OSADLObligationConnector();
        return osadlConnector.getText(licenseId).flatMap(obligationText -> {
            if (obligationText == null || obligationText.trim().isEmpty()) {
                return Mono.empty();
            }
            Obligation obligation = new Obligation();
            obligation.setId(TMP_OBLIGATION_ID_PREFIX + UUID.randomUUID());
            obligation.setText(obligationText);
            obligation.setTitle(licenseId);
            obligation.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);
            obligation.setObligationType(ObligationType.OBLIGATION);
            obligation.setDevelopment(false);
            obligation.setDistribution(false);
            obligation.addToWhitelist(user.getDepartment());
            obligation.setExternalIds(Collections.singletonMap(EXTERNAL_ID_OSADL, licenseId));
            return Mono.just(obligation);
        }).onErrorResume(HttpClientErrorException.class, e -> {
            log.error("Got 404 while fetching OSADL data for license: {}", licenseId);
            return Mono.empty();
        }).onErrorResume(ResourceClassNotFoundException.class, e -> {
            log.error("Got 500 while fetching OSADL data for license: {}", licenseId);
            return Mono.empty();
        }).doOnError(e -> {
            log.error("Unhandled exception while fetching OSADL data for license: {}", licenseId, e);
        });
	}

	@Override
	public JSONObject parseText(String obligationText) {
        String[] arrayLines = obligationText.split("\n");
		List<String> jsonText = setLinePath(setLineLevel(arrayLines));
		return buildTreeObject(jsonText);
	}

    @Override
    protected Mono<String> getText(String licenseId) {
        String obligationURL = generateURL(licenseId);
		try {
            return BackendUtils.getUriBody(new URI(obligationURL));
        } catch (URISyntaxException e) {
            log.error("Could not get OSADL License for: {}", licenseId);
            return Mono.empty();
        }
    }

	private List<String> setLineLevel(String[] arraylines) {
		List<String> refinedLines = new ArrayList<>();
		for (int i = 0; i < arraylines.length; i++) {
			if (arraylines[i].isEmpty()) {
				continue;
			}
			int currentLevel = getLevel(arraylines[i]);
			String lineWithLevel = "{ 'id': '" + i + "', 'text': '"
                    + arraylines[i].replaceAll("\t","")
                    .replaceAll("'", "\\\\'")
                    + "', 'level': '" + currentLevel
                    + "', 'path': '-1'}";
			refinedLines.add(lineWithLevel);
		}
		return refinedLines;
	}

	private int getLevel(String line) {
		return line.length() - line.replace("\t","").length();
	}

	private List<String> setLinePath(List<String> lines) {
		List<String> refinedLines = new ArrayList<>();
		int i = 0;
		for (String line : lines) {
			String parentLinePath = "-1";
			try {
				JSONObject currentLine = new JSONObject(line);
				int parentLineId = getParentLineId(currentLine, lines);
				if (parentLineId >= 0) {
					JSONObject parentLine = new JSONObject(lines.get(parentLineId));
					parentLinePath = parentLine.get("path").toString();
				}
				currentLine.remove("path");
				String path = parentLinePath.replace("@", "") + "@" + currentLine.get("id").toString();
				currentLine.put("path", path.trim());
				lines.set(i, currentLine.toString());
				i++;
				String jsonLine = "{ 'id': '" + currentLine.get("id") + "', 'text': '" +
                        currentLine.get("text").toString().replaceAll("'", "\\\\'") +
                        "', 'level': '" + currentLine.get("level") + "', 'path': '" + currentLine.get("path")+ "'}";
				refinedLines.add(jsonLine);
			} catch (Exception e) {
                log.error("Can not set line path: {}", line);
				return null;
			}
		}
		return refinedLines;
	}

	private int getParentLineId(JSONObject currentLine, List<String> lines) {
		int currentLevel = Integer.parseInt(currentLine.get("level").toString());
		for (int i = Integer.parseInt(currentLine.get("id").toString()); i >= 0; i--) {
			try {
				JSONObject lineCheck = new JSONObject(lines.get(i));
				if (Integer.parseInt(lineCheck.get("level").toString()) == currentLevel - 1) {
					return i;
				}
			} catch (Exception e) {
				log.error("Can not get parent line id from: " + currentLine);
				return -1;
			}
		}
		return -1;
	}

	private JSONObject buildTreeObject(List<String> lines) {
		try {
			String rootNodeText = "{'val': ['ROOT'], 'children': [], 'path': '-1'}";
			JSONObject rootNode = new JSONObject(rootNodeText);
			return removeField(addNode(rootNode, lines), "path");
		} catch (Exception e) {
            log.error("Can not build tree object from: {}", lines);
			return null;
		}
	}

	private JSONObject removeField(JSONObject rootNode, String field) {
        if (rootNode == null) {
            return null;
        }
		rootNode.remove(field);
		for (int i = 0; i < rootNode.getJSONArray("children").length(); i++) {
			JSONObject contactObject = rootNode.getJSONArray("children").getJSONObject(i);
			removeField(contactObject, field);
		}
		return rootNode;
	}

	private JSONObject addNode(JSONObject rootNode, List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			try {
				JSONObject line = new JSONObject(lines.get(i));
				if (line.get("path").toString().split("@")[0].equals(rootNode.get("path").toString().replace("@", ""))) {
					String childNode;
					List<String> text = parseSentenceElement(line.get("text").toString());
					if (text.get(0).equals("Obligation")) {
						childNode = "{'val': ['" +text.get(0)+ "', '" +text.get(1)+ "', '" +text.get(2)+ "', '" +text.get(3)+"'], 'children': [], 'path': '" +line.get("path")+ "'}";
					} else {
						childNode = "{'val': ['" +text.get(0)+ "', '" +text.get(1)+ "'], 'children': [], 'path': '" +line.get("path")+ "'}";
					}
					rootNode.getJSONArray("children").put(new JSONObject(childNode));
				}
			} catch (Exception e) {
                log.error("Can not add node from: {}", lines);
				return null;
			}
		}
		for (int i = 0; i < rootNode.getJSONArray("children").length(); i++) {
			JSONObject contactObject = rootNode.getJSONArray("children").getJSONObject(i);
			addNode(contactObject, lines);
		}
		return rootNode;
	}

	private List<List<String>> getProperty() {
		List<List<String>> keywords = new ArrayList<List<String>>();
		List<String> obligation = new ArrayList<>(Arrays.asList("YOU MUST", "YOU MUST NOT"));
		List<String> other = new ArrayList<>(Arrays.asList(
		"ATTRIBUTE NOT",
		"ATTRIBUTE",
		"COMPATIBILITY",
		"COPYLEFT CLAUSE",
		"DEPENDING COMPATIBILITY",
		"EITHER IF",
		"EITHER",
		"EXCEPT IF NOT",
		"EXCEPT IF",
		"IF",
		"INCOMPATIBILITY",
		"OR IF",
		"OR",
		"PATENT HINTS",
		"USE CASE",
		"YOU MUST NOT",
		"YOU MUST"
		));
		keywords.add(obligation);
		keywords.add(other);
		return keywords;
	}

	private List<String> sortArray(List<List<String>> arrList, String type) {
		List<String> both = new ArrayList<>();
		for (List<String> arr: arrList) {
            both.addAll(arr);
		}

		if ( type.equals("ASC")) {
			both.sort(Comparator.comparingInt(String::length));
		} else {
			both.sort((String s1, String s2) -> s2.length() - s1.length());
		}
		return both;
	}

	private List<String> parseSentenceElement(String text) {
		text = text.trim();
		List<List<String>> data = getProperty();
		List<String> allKeywords = sortArray(data, "1");
		List<String> obligationProperty = data.getFirst();
		for (int i = 0; i < allKeywords.size(); i++ ) {
			if (text.startsWith(allKeywords.get(i))) {
				if (obligationProperty.contains(allKeywords.get(i))) {
					String languageElement = allKeywords.get(i);
					String actionAndObjectText = text.substring(languageElement.length()).trim();
					String actions = getActions(actionAndObjectText);
					String object = actionAndObjectText.substring(actions.length()).trim();
					return new ArrayList<>(Arrays.asList("Obligation", languageElement, actions, object));
				} else {
					String type = allKeywords.get(i);
					return new ArrayList<>(Arrays.asList(type, text.substring(type.length()).trim()));
				}
			}
		}

		String type = "";
		String value = "";
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++) {
			if (words[i].equals(words[i].toUpperCase())) {
				type = type + ' ' + words[i];
			} else {
				break;
			}
		}
		type = type.trim();
		if (type.isEmpty()) {
			type = words[0];
		}
		value = text.substring(type.length());
		return new ArrayList<>(Arrays.asList(type, value));
	}

	private String getActions(String text) {
		if (text.contains("OR")) {
			List<String> words = Arrays.asList(text.split(" "));
			List<Integer> ORpos = new ArrayList<>();
			for (int i = 0; i < words.size(); i++) {
				if (words.get(i).equals("OR")) {
					ORpos.add(i);
				}
			}

			if (ORpos.getFirst() != 1) {
				return text.split(" ")[0];
			} else {
				int lastORPos = ORpos.getFirst();
				for (int i = 0; i < ORpos.size() - 1; i++) {
					if (ORpos.get(i + 1) == ORpos.get(i) + 2) {
						lastORPos = ORpos.get(i + 1);
					} else {
						break;
					}
				}

				List<String> result = new ArrayList<>();
				for (int i = 0; i < lastORPos + 1; i++) {
					result.add(words.get(i));
				}

				if (lastORPos < words.size() - 1) {
					result.add(words.get(lastORPos + 1));
				}
				return String.join(" ", result);
			}
		} else {
			return text.split(" ")[0];
		}
	}

    /**
     * Get the list of licenses for which OSADL is providing obligations.
     * @return List of licenses based on CHECKLISTS URL
     */
    public List<String> getOsadlLicenses() {
        String allUrls;
        List<String> allLicenses = new ArrayList<>();
        try {
            URI osadlAllList = new URI(CHECKLISTS);
            allUrls = BackendUtils.getUriBody(osadlAllList).block();
        } catch (URISyntaxException e) {
            log.error("Could not get the OSADL list from: {}", CHECKLISTS);
            return allLicenses;
        }
        if (allUrls == null) {
            return allLicenses;
        }
        for (String url : allUrls.split("\n")) {
            if (url.startsWith(BASE_URL) && url.endsWith(OSADL_FORMAT)) {
                allLicenses.add(url.replaceFirst(BASE_URL, "").replace(OSADL_FORMAT, ""));
            }
        }
        return allLicenses;
    }
}
