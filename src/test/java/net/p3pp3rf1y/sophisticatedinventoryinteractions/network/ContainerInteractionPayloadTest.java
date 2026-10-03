package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerInteractionPayloadTest {
	@Test
	void preservesTheMaximumNumberOfExcludedSlots() {
		ContainerInteractionPayload payload = new ContainerInteractionPayload(InteractionActionType.SORT_CONTAINER, true, SortBy.NAME,
				IntStream.range(0, 512).boxed().toList());
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

		ContainerInteractionPayload.encode(payload, buffer);

		assertEquals(payload, ContainerInteractionPayload.decode(buffer));
	}

	@Test
	void rejectsMoreThanTheMaximumNumberOfExcludedSlots() {
		assertThrows(IllegalArgumentException.class,
				() -> new ContainerInteractionPayload(InteractionActionType.SORT_CONTAINER, true, SortBy.NAME, IntStream.range(0, 513).boxed().toList()));
	}
}
