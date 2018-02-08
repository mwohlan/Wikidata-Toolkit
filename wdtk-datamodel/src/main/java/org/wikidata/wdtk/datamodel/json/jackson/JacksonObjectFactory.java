package org.wikidata.wdtk.datamodel.json.jackson;

/*
 * #%L
 * Wikidata Toolkit Data Model
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.wikidata.wdtk.datamodel.helpers.DatamodelConverter;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.datamodel.json.jackson.datavalues.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory implementation to create Jackson versions of the datamodel objects,
 * where available.
 *
 * @author Markus Kroetzsch
 *
 */
public class JacksonObjectFactory implements DataObjectFactory {

	private final DatamodelConverter dataModelConverter = new DatamodelConverter(
			this);

	@Override
	public ItemIdValue getItemIdValue(String id, String siteIri) {
		JacksonValueItemId result = new JacksonValueItemId(new JacksonInnerEntityId(id), siteIri);
		return result;
	}

	@Override
	public PropertyIdValue getPropertyIdValue(String id, String siteIri) {
		JacksonValuePropertyId result = new JacksonValuePropertyId(new JacksonInnerEntityId(id), siteIri);
		return result;
	}

	@Override
	public DatatypeIdValue getDatatypeIdValue(String id) {
		return new JacksonDatatypeId(JacksonDatatypeId.getJsonDatatypeFromDatatypeIri(id));
	}

	@Override
	public TimeValue getTimeValue(long year, byte month, byte day, byte hour,
			byte minute, byte second, byte precision, int beforeTolerance,
			int afterTolerance, int timezoneOffset, String calendarModel) {
		JacksonInnerTime innerTime = new JacksonInnerTime(year, month, day,
				hour, minute, second, timezoneOffset, beforeTolerance,
				afterTolerance, precision, calendarModel);
		JacksonValueTime result = new JacksonValueTime(innerTime);
		return result;
	}

	@Override
	public GlobeCoordinatesValue getGlobeCoordinatesValue(double latitude,
			double longitude, double precision, String globeIri) {
		if (precision <= 0) {
			throw new IllegalArgumentException(
					"Coordinates precision must be non-zero positive. Given value: "
							+ precision);
		}
		JacksonInnerGlobeCoordinates innerCoordinates = new JacksonInnerGlobeCoordinates(
				latitude,
				longitude,
				precision,
				globeIri);
		JacksonValueGlobeCoordinates result = new JacksonValueGlobeCoordinates(innerCoordinates);
		return result;
	}

	@Override
	public StringValue getStringValue(String string) {
		JacksonValueString result = new JacksonValueString(string);
		return result;
	}

	@Override
	public MonolingualTextValue getMonolingualTextValue(String text,
			String languageCode) {
		JacksonInnerMonolingualText innerMtlv = new JacksonInnerMonolingualText(
				languageCode, text);
		JacksonValueMonolingualText result = new JacksonValueMonolingualText(innerMtlv);
		return result;
	}


	@Override
	public QuantityValue getQuantityValue(BigDecimal numericValue) {
		return getQuantityValue(numericValue, null, null, "");
	}

	@Override
	public QuantityValue getQuantityValue(BigDecimal numericValue,
			BigDecimal lowerBound, BigDecimal upperBound) {
		return getQuantityValue(numericValue, lowerBound, upperBound, "");
	}

	@Override
	public QuantityValue getQuantityValue(BigDecimal numericValue, String unit) {
		return getQuantityValue(numericValue, null, null, unit);
	}

	@Override
	public QuantityValue getQuantityValue(BigDecimal numericValue,
			BigDecimal lowerBound, BigDecimal upperBound, String unit) {
		JacksonInnerQuantity innerQuantity = new JacksonInnerQuantity(
				numericValue, upperBound, lowerBound, unit);
		JacksonValueQuantity result = new JacksonValueQuantity(innerQuantity);
		return result;
	}

	/**
	 * Creates a {@link JacksonValueSnak}. Value snaks in JSON need to know the
	 * datatype of their property, which is not given in the parameters of this
	 * method. The snak that will be returned will use a default type based on
	 * the kind of value that is used (usually the "simplest" type for that
	 * value). This may not be desired.
	 *
	 * @see DataObjectFactory#getValueSnak(PropertyIdValue, Value)
	 */
	@Override
	public ValueSnak getValueSnak(PropertyIdValue propertyId, Value value) {
		JacksonValueSnak result = new JacksonValueSnak(
				propertyId.getId(),
				getJsonPropertyTypeForValueType(value),
				value,
				propertyId.getSiteIri());
		return result;
	}

	@Override
	public SomeValueSnak getSomeValueSnak(PropertyIdValue propertyId) {
		JacksonSomeValueSnak result = new JacksonSomeValueSnak(
				propertyId.getId(),
				propertyId.getSiteIri());
		return result;
	}

	@Override
	public NoValueSnak getNoValueSnak(PropertyIdValue propertyId) {
		JacksonNoValueSnak result = new JacksonNoValueSnak(
				propertyId.getId(),
				propertyId.getSiteIri());
		return result;
	}

	@Override
	public SnakGroup getSnakGroup(List<? extends Snak> snaks) {
		List<Snak> snakList = new ArrayList<>(snaks.size());
		for(Snak snak : snaks) {
			snakList.add(snak);
		}
		return new SnakGroupFromJson(snakList);
	}

	@Override
	public Claim getClaim(EntityIdValue subject, Snak mainSnak,
			List<SnakGroup> qualifiers) {
		// Jackson claims cannot exist without a statement.
		Statement statement = getStatement(
				subject, mainSnak, qualifiers,
				Collections.<Reference> emptyList(), StatementRank.NORMAL,
				"empty id 12345");
		return statement.getClaim();
	}

	@Override
	public Reference getReference(List<SnakGroup> snakGroups) {
		return new JacksonReference(snakGroups);
	}

	@Override
	public Statement getStatement(Claim claim,
			List<Reference> references, StatementRank rank,
			String statementId) {
		return getStatement(claim.getSubject(), claim.getMainSnak(), claim.getQualifiers(),
				references, rank, statementId);
	}
	
	private Statement getStatement(EntityIdValue subject, Snak mainSnak, List<SnakGroup> qualifiers,
			List<Reference> references, StatementRank rank, String statementId) {

		return new JacksonStatement(statementId,
				rank, mainSnak, qualifiers,
				references, subject);
	}

	@Override
	public StatementGroup getStatementGroup(List<Statement> statements) {
		List<Statement> newStatements = new ArrayList<>(statements.size());
		for (Statement statement : statements) {
			if (statement instanceof JacksonPreStatement) {
				newStatements.add(statement);
			} else {
				newStatements.add(this.dataModelConverter.copy(statement));
			}
		}
		return new StatementGroupFromJson(newStatements);
	}

	@Override
	public SiteLink getSiteLink(String title, String siteKey,
			List<String> badges) {
		JacksonSiteLink result = new JacksonSiteLink(
				title, siteKey, badges);
		return result;
	}

	@Override
	public PropertyDocument getPropertyDocument(PropertyIdValue propertyId,
			List<MonolingualTextValue> labels,
			List<MonolingualTextValue> descriptions,
			List<MonolingualTextValue> aliases, DatatypeIdValue datatypeId) {
		return getPropertyDocument(propertyId, labels, descriptions, aliases,
				Collections.<StatementGroup> emptyList(), datatypeId, 0);
	}

	@Override
	public PropertyDocument getPropertyDocument(PropertyIdValue propertyId,
			List<MonolingualTextValue> labels,
			List<MonolingualTextValue> descriptions,
			List<MonolingualTextValue> aliases,
			List<StatementGroup> statementGroups, DatatypeIdValue datatypeId) {
		return getPropertyDocument(propertyId, labels, descriptions, aliases,
				statementGroups, datatypeId, 0);
	}

	@Override
	public PropertyDocument getPropertyDocument(PropertyIdValue propertyId,
			List<MonolingualTextValue> labels,
			List<MonolingualTextValue> descriptions,
			List<MonolingualTextValue> aliases,
			List<StatementGroup> statementGroups, DatatypeIdValue datatypeId,
			long revisionId) {
		JacksonPropertyDocument result = new JacksonPropertyDocument(
				propertyId, labels, descriptions, aliases, statementGroups,
				datatypeId,	revisionId);
		return result;
	}

	@Override
	public ItemDocument getItemDocument(ItemIdValue itemIdValue,
			List<MonolingualTextValue> labels,
			List<MonolingualTextValue> descriptions,
			List<MonolingualTextValue> aliases,
			List<StatementGroup> statementGroups,
			Map<String, SiteLink> siteLinks) {
		return getItemDocument(itemIdValue, labels, descriptions, aliases,
				statementGroups, siteLinks, 0);
	}

	@Override
	public ItemDocument getItemDocument(ItemIdValue itemIdValue,
			List<MonolingualTextValue> labels,
			List<MonolingualTextValue> descriptions,
			List<MonolingualTextValue> aliases,
			List<StatementGroup> statementGroups,
			Map<String, SiteLink> siteLinks, long revisionId) {

		JacksonItemDocument result = new JacksonItemDocument(
				itemIdValue, labels, descriptions, aliases, statementGroups,
				siteLinks.values().stream().collect(Collectors.toList()), revisionId);

		return result;
	}

	private String getJsonPropertyTypeForValueType(Value value) {
		if (value instanceof TimeValue) {
			return JacksonDatatypeId.JSON_DT_TIME;
		} else if (value instanceof ItemIdValue) {
			return JacksonDatatypeId.JSON_DT_ITEM;
		} else if (value instanceof PropertyIdValue) {
			return JacksonDatatypeId.JSON_DT_PROPERTY;
		} else if (value instanceof StringValue) {
			return null;
		} else if (value instanceof GlobeCoordinatesValue) {
			return JacksonDatatypeId.JSON_DT_GLOBE_COORDINATES;
		} else if (value instanceof QuantityValue) {
			return JacksonDatatypeId.JSON_DT_QUANTITY;
		} else if (value instanceof MonolingualTextValue) {
			return JacksonDatatypeId.JSON_DT_MONOLINGUAL_TEXT;
		} else {
			throw new UnsupportedOperationException("Unsupported value type "
					+ value.getClass());
		}
	}
}
