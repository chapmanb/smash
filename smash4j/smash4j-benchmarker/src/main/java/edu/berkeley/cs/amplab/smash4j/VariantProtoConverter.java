package edu.berkeley.cs.amplab.smash4j;

import com.google.api.services.genomics.model.Call;
import com.google.api.services.genomics.model.Variant;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import edu.berkeley.cs.amplab.smash4j.Smash4J.VariantProto;
import edu.berkeley.cs.amplab.vcfparser.VcfRecord;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class VariantProtoConverter<X> {

  private static final Function<Map<String, List<String>>, VariantProto.Multimap> CONVERT_MULTIMAP =
      new Function<Map<String, List<String>>, VariantProto.Multimap>() {

        private final Function<Map.Entry<String, List<String>>, VariantProto.Multimap.Entry>
            convertEntry =
            new Function<Map.Entry<String, List<String>>, VariantProto.Multimap.Entry>() {
              @Override
              public VariantProto.Multimap.Entry apply(Map.Entry<String, List<String>> entry) {
                return VariantProto.Multimap.Entry.newBuilder()
                    .setKey(entry.getKey())
                    .addAllValue(entry.getValue())
                    .build();
              }
            };

        @Override public VariantProto.Multimap apply(Map<String, List<String>> multimap) {
          return VariantProto.Multimap.newBuilder()
              .addAllEntry(Iterables.transform(multimap.entrySet(), convertEntry))
              .build();
        }
      };

  private static final Function<Call, VariantProto.Multimap> CALL_TO_MULTIMAP = Functions.compose(
      CONVERT_MULTIMAP,
      new Function<Call, Map<String, List<String>>>() {
        @Override public Map<String, List<String>> apply(Call call) {
          return call.getInfo();
        }
      });

  public static final VariantProtoConverter<VcfRecord> VCF_RECORD_CONVERTER =
      new VariantProtoConverter<VcfRecord>() {
        @Override public VariantProto convert(final VcfRecord record) {
          return VariantProto.newBuilder()
              .addAllAlternateBases(record.alt())
              .addAllCall(Iterables.transform(record.samples(), Functions.compose(CONVERT_MULTIMAP,
                  new Function<List<String>, Map<String, List<String>>>() {
                    @Override public Map<String, List<String>> apply(List<String> call) {
                      ImmutableMap.Builder<String, List<String>> map = ImmutableMap.builder();
                      for (Iterator<String>
                          keys = record.format().iterator(), values = call.iterator();
                          keys.hasNext() && values.hasNext();
                          map.put(keys.next(), Arrays.asList(values.next().split(","))));
                      return map.build();
                    }
                  })))
              .setContig(record.chrom())
              .addAllNames(record.ids())
              .setPosition(record.pos())
              .setReferenceBases(record.ref())
              .setInfo(CONVERT_MULTIMAP.apply(record.info()))
              .build();
        }
      };

  public static final VariantProtoConverter<Variant> VARIANT_CONVERTER =
      new VariantProtoConverter<Variant>() {
        @Override public VariantProto convert(Variant variant) {
          return VariantProto.newBuilder()
              .addAllAlternateBases(variant.getAlternateBases())
              .addAllCall(Iterables.transform(variant.getCalls(), CALL_TO_MULTIMAP))
              .setContig(variant.getContig())
              .setInfo(CONVERT_MULTIMAP.apply(variant.getInfo()))
              .addAllNames(variant.getNames())
              .setPosition(variant.getPosition())
              .setReferenceBases(variant.getReferenceBases())
              .build();
        }
      };

  public abstract VariantProto convert(X object);
}