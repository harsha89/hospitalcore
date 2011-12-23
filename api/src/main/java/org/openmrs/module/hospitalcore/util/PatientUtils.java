/**
 *  Copyright 2010 Society for Health Information Systems Programmes, India (HISP India)
 *
 *  This file is part of Hospital-core module.
 *
 *  Hospital-core module is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  Hospital-core module is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Hospital-core module.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/

package org.openmrs.module.hospitalcore.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.hospitalcore.HospitalCoreService;

public class PatientUtils {

	public static final String MODULE_ID = "hospitalcore.";
	public final static String PATIENT_ATTRIBUTE_CATEGORY = "Patient Category";
	public final static String PATIENT_ATTRIBUTE_BPL_NUMBER = "BPL Number";
	public final static String PATIENT_ATTRIBUTE_RSBY_NUMBER = "RSBY Number";
	public final static String PATIENT_AGE_CATEGORY = MODULE_ID + "ageCategory";

	/**
	 * Get patient category printout based on patient's category
	 * 
	 * @param patient
	 * @return
	 */
	public static String getPatientCategory(Patient patient) {
		String category = "";

		String patientCategory = getPatientAttribute(patient,
				PATIENT_ATTRIBUTE_CATEGORY);
		if (!StringUtils.isBlank(patientCategory)) {
			if (patientCategory.contains("General"))
				category += "General";
			String RSBYNo = getPatientAttribute(patient,
					PATIENT_ATTRIBUTE_RSBY_NUMBER);
			String BPLNo = getPatientAttribute(patient,
					PATIENT_ATTRIBUTE_BPL_NUMBER);
			if (!StringUtils.isBlank(RSBYNo)) {
				category += "RSBY";
			} else if (!StringUtils.isBlank(BPLNo)) {
				category += "BPL";
			}
			if (patientCategory.contains("MLC")) {
				category += ", MLC";
			}
		}

		return category;
	}

	/**
	 * Get the fullname of patient
	 * 
	 * @param patient
	 * @return
	 */
	public static String getFullName(Patient patient) {
		String fullName = "";

		if (!StringUtils.isBlank(patient.getGivenName())) {
			fullName += patient.getGivenName() + " ";
		}

		if (!StringUtils.isBlank(patient.getMiddleName())) {
			fullName += patient.getMiddleName() + " ";
		}

		if (!StringUtils.isBlank(patient.getFamilyName())) {
			fullName += patient.getFamilyName();
		}

		fullName = StringUtils.trim(fullName);
		return fullName;
	}

	/**
	 * Get the age category based on patient's age
	 * 
	 * @param patient
	 * @return
	 */
	public static String getAgeCategory(Patient patient) {
		String ageCategories = GlobalPropertyUtil.getString(
				PATIENT_AGE_CATEGORY, "null");
		try {
			String[] categories = ageCategories.split(";");
			for (String category : categories) {
				String[] parts = category.split(":");
				String categoryName = parts[1];
				String categoryCondition = parts[0];
				String[] conditions = categoryCondition.split("-");
				Integer lower = Integer.parseInt(conditions[0]);
				Integer upper = Integer.parseInt(conditions[1]);
				if ((lower <= patient.getAge()) && (patient.getAge() <= upper))
					return categoryName;
			}
		} catch (Exception e) {
			System.out.println("Error while generating age category!");
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get patient attribute
	 * 
	 * @param patient
	 * @param attributeNameType
	 * @return
	 */
	public static String getPatientAttribute(Patient patient,
			String attributeNameType) {
		String value = null;
		PersonAttributeType pat = Context.getPersonService()
				.getPersonAttributeTypeByName(attributeNameType);
		PersonAttribute pa = patient.getAttribute(pat);
		if (pa != null) {
			value = pa.getValue();
		}
		return value;
	}

	public static void removePatientAttribute(Patient patient,
			String attributeNameType) {
		PersonAttributeType pat = Context.getPersonService()
				.getPersonAttributeTypeByName(attributeNameType);
		PersonAttribute pa = patient.getAttribute(pat);
		patient.removeAttribute(pa);
		Context.getPatientService().savePatient(patient);
	}

	public static Map<String, String> getAttributes(Patient patient) {
		Map<String, String> attributes = new HashMap<String, String>();

		for (String key : patient.getAttributeMap().keySet()) {
			attributes.put(patient.getAttributeMap().get(key)
					.getAttributeType().getName(), patient.getAttributeMap()
					.get(key).getValue());
		}

		// get last encounter
		List<EncounterType> types = new ArrayList<EncounterType>();
		EncounterType reginit = Context.getEncounterService().getEncounterType(
				1);
		types.add(reginit);
		EncounterType revisit = Context.getEncounterService().getEncounterType(
				2);
		types.add(revisit);
		Encounter lastVisit = Context.getService(HospitalCoreService.class)
				.getLastVisitEncounter(patient, types);

		if (lastVisit != null) {
			for (Obs obs : lastVisit.getAllObs()) {
				if (!obs.isVoided()) {
					if (obs.getConcept().getDatatype().getName()
							.equalsIgnoreCase("Coded")) {
						attributes.put(obs.getConcept().getName().getName(),
								obs.getValueCoded().getName().getName());
					} else if (obs.getConcept().getDatatype().getName()
							.equalsIgnoreCase("Text")) {
						attributes.put(obs.getConcept().getName().getName(),
								obs.getValueText());
					}

				}
			}
		}

		return attributes;
	}

	/**
	 * Estimate patient age
	 * @param patient
	 * @return
	 */
	public static String estimateAge(Patient patient) {
		return estimateAge(patient.getBirthdate());
	}
	
	/**
	 * Estimate age using birthdate
	 * 
	 * @param birthdate
	 * @return
	 * @throws ParseException
	 */
	public static String estimateAge(Date date) {

		String age = "~ ";
		long diff = Math.abs(date.getTime() - (new Date()).getTime())
				/ (1000 * 60 * 60 * 24);
		long yearDiff = diff / 365;

		if (yearDiff > 0) {
			if (yearDiff == 1) {
				age += yearDiff + " year ";
			} else {
				age += yearDiff + " years ";
			}
		} else {
			long monthDiff = (diff % 365) / 30;
			
			if (monthDiff > 0) {
				if (monthDiff == 1) {
					age += monthDiff + " month ";
				} else {
					age += monthDiff + " months ";
				}
			}

			long dateDiff = (diff % 365) % 30;
			if (dateDiff >= 0) {
				if ((dateDiff == 1) || (dateDiff == 0)){
					age += dateDiff + " day ";
				} else {
					age += dateDiff + " days ";
				}
			}
		}
		return age;
	}
}
